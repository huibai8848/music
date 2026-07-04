package com.example.music.service.impl;

import cn.hutool.core.io.FileUtil;
import com.example.music.constant.ErrorCode;
import com.example.music.entity.Album;
import com.example.music.entity.Artist;
import com.example.music.entity.Comment;
import com.example.music.entity.Song;
import com.example.music.exception.BusinessException;
import com.example.music.mapper.*;
import com.example.music.service.AdminContentService;
import com.example.music.service.FileService;
import com.example.music.service.NotificationService;
import com.example.music.vo.AlbumVO;
import com.example.music.vo.ArtistVO;
import com.example.music.vo.SongVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 管理后台内容管理服务实现
 * <p>
 * 管理歌曲/专辑/艺人/评论，包含审核会员上传歌曲等功能。
 * 操作完成后需记录操作日志（通过 AOP 或手动调用 OperationLogMapper）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminContentServiceImpl implements AdminContentService {

    private final SongMapper songMapper;
    private final AlbumMapper albumMapper;
    private final ArtistMapper artistMapper;
    private final CommentMapper commentMapper;
    private final FileService fileService;
    private final FavoriteMapper favoriteMapper;
    private final NotificationService notificationService;

    // ==================== 歌曲管理 ====================

    @Override
    public Map<String, Object> listSongs(String status, String keyword, int page, int size) {
        int offset = (page - 1) * size;

        List<Song> songs;
        long total;
        if (keyword != null && !keyword.isEmpty()) {
            songs = songMapper.search(keyword, offset, size);
            total = songMapper.countSearch(keyword);
        } else {
            songs = songMapper.selectList(offset, size, null, null, null, null, null, status);
            total = songMapper.countTotal(null, null, null, null, null, status);
        }

        List<SongVO> voList = songs.stream()
                .map(song -> {
                    SongVO vo = SongVO.fromEntity(song);
                    // 填充艺人名（管理端表格展示用）
                    if (song.getArtistId() != null) {
                        Artist artist = artistMapper.selectById(song.getArtistId());
                        if (artist != null) {
                            vo.setArtistName(artist.getName());
                        }
                    }
                    return vo;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("records", voList);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditSong(Long adminId, Long songId, String status, String rejectReason) {
        Song song = songMapper.selectById(songId);
        if (song == null) {
            throw new BusinessException(ErrorCode.SONG_NOT_FOUND);
        }

        String oldStatus = song.getStatus();
        // 使用乐观锁更新：仅 PENDING 状态可被审核，防止并发重复审核
        int affected = songMapper.auditStatus(songId, status);
        if (affected == 0) {
            throw new BusinessException(ErrorCode.ADMIN_SONG_ALREADY_AUDITED);
        }
        log.info("管理员 {} 审核歌曲 {} ({}): status={}, reason={}",
                adminId, songId, song.getTitle(), status, rejectReason);

        // 审核后通知上传者和艺人关注者 —— 延迟到事务提交后执行
        String finalStatus = status;
        String songTitle = song.getTitle();
        Long uploaderId = song.getUploaderId();
        Long artistId = song.getArtistId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // 1. 通知上传者审核结果
                if (uploaderId != null) {
                    String resultMsg = "ACTIVE".equals(finalStatus)
                            ? "您的上传歌曲《" + songTitle + "》已通过审核"
                            : "您的上传歌曲《" + songTitle + "》未通过审核" +
                              (rejectReason != null ? "，原因：" + rejectReason : "");
                    notificationService.createNotification(
                            uploaderId,
                            "SONG_REVIEW",
                            "歌曲审核结果",
                            resultMsg,
                            "SONG",
                            songId
                    );
                }

                // 2. 如果审核通过，通知艺人的关注者
                if ("ACTIVE".equals(finalStatus) && artistId != null) {
                    notifyArtistFollowers(artistId, songTitle, songId);
                }
            }
        });
    }

    /**
     * 通知艺人的关注者：该艺人有新歌发布
     */
    private void notifyArtistFollowers(Long artistId, String songTitle, Long songId) {
        try {
            List<Long> followerIds = favoriteMapper.selectUserIdsByTarget("ARTIST", artistId);
            if (followerIds == null || followerIds.isEmpty()) {
                return;
            }
            // 获取艺人名称
            Artist artist = artistMapper.selectById(artistId);
            String artistName = artist != null ? artist.getName() : "未知艺人";

            for (Long followerId : followerIds) {
                notificationService.createNotification(
                        followerId,
                        "SYSTEM",
                        "关注艺人有新歌",
                        "您关注的艺人 " + artistName + " 发布了新歌曲《" + songTitle + "》",
                        "SONG",
                        songId
                );
            }
            log.info("已通知 {} 位关注者关于艺人 {} 的新歌: songId={}",
                    followerIds.size(), artistName, songId);
        } catch (Exception e) {
            log.error("通知艺人关注者失败: artistId={}, songId={}", artistId, songId, e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SongVO createSong(Long adminId, Song song) {
        if (song.getTitle() == null || song.getTitle().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "歌曲名不能为空");
        }
        // 默认审核通过
        if (song.getStatus() == null || song.getStatus().isEmpty()) {
            song.setStatus("ACTIVE");
        }
        if (song.getDuration() == null) {
            song.setDuration(0);
        }
        if (song.getPlayCount() == null) {
            song.setPlayCount(0L);
        }
        if (song.getAudioUrl() == null) {
            song.setAudioUrl("");
        }
        if (song.getCoverUrl() == null) {
            song.setCoverUrl("");
        }
        if (song.getLyricUrl() == null) {
            song.setLyricUrl("");
        }
        song.setUploaderId(adminId);
        songMapper.insert(song);
        log.info("管理员 {} 创建歌曲: id={}, title={}, status={}", adminId, song.getId(), song.getTitle(), song.getStatus());

        SongVO vo = SongVO.fromEntity(song);
        fillArtistName(vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SongVO updateSong(Long adminId, Long id, Song song) {
        Song existing = songMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.SONG_NOT_FOUND);
        }
        song.setId(id);
        songMapper.update(song);
        log.info("管理员 {} 更新歌曲: id={}, title={}", adminId, id, song.getTitle());

        SongVO vo = SongVO.fromEntity(songMapper.selectById(id));
        fillArtistName(vo);
        return vo;
    }

    @Override
    public SongVO getSongDetail(Long id) {
        Song song = songMapper.selectById(id);
        if (song == null) {
            throw new BusinessException(ErrorCode.SONG_NOT_FOUND);
        }
        SongVO vo = SongVO.fromEntity(song);
        fillArtistName(vo);
        return vo;
    }

    /**
     * 填充 SongVO 的艺人名
     */
    private void fillArtistName(SongVO vo) {
        if (vo.getArtistId() != null) {
            Artist artist = artistMapper.selectById(vo.getArtistId());
            if (artist != null) {
                vo.setArtistName(artist.getName());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSong(Long adminId, Long songId) {
        Song song = songMapper.selectById(songId);
        if (song == null) {
            throw new BusinessException(ErrorCode.SONG_NOT_FOUND);
        }
        songMapper.deleteById(songId);
        log.info("管理员 {} 删除歌曲 {}: {}", adminId, songId, song.getTitle());
    }

    // ==================== 专辑管理 ====================

    @Override
    public Map<String, Object> listAlbums(String keyword, int page, int size) {
        int offset = (page - 1) * size;

        List<Album> albums;
        long total;
        if (keyword != null && !keyword.isEmpty()) {
            albums = albumMapper.searchByName(keyword);
            total = albums.size();
            // 手动内存分页
            int from = Math.min(offset, albums.size());
            int to = Math.min(offset + size, albums.size());
            albums = albums.subList(from, to);
        } else {
            albums = albumMapper.selectList(offset, size);
            total = albumMapper.countTotal();
        }

        List<AlbumVO> voList = albums.stream()
                .map(album -> {
                    AlbumVO vo = AlbumVO.fromEntity(album);
                    if (album.getArtistId() != null) {
                        Artist artist = artistMapper.selectById(album.getArtistId());
                        if (artist != null) {
                            vo.setArtistName(artist.getName());
                        }
                    }
                    // 填充歌曲数量
                    vo.setSongCount((int) songMapper.countByAlbumId(album.getId()));
                    return vo;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("records", voList);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAlbum(Long adminId, Long albumId) {
        Album album = albumMapper.selectById(albumId);
        if (album == null) {
            throw new BusinessException(ErrorCode.ALBUM_NOT_FOUND);
        }
        albumMapper.deleteById(albumId);
        log.info("管理员 {} 删除专辑 {}: {}", adminId, albumId, album.getTitle());
    }

    // ==================== 艺人管理 ====================

    @Override
    public Map<String, Object> listArtists(String keyword, int page, int size) {
        int offset = (page - 1) * size;

        List<Artist> artists;
        long total;
        if (keyword != null && !keyword.isEmpty()) {
            artists = artistMapper.searchByName(keyword, offset, size);
            total = artistMapper.countSearch(keyword);
        } else {
            artists = artistMapper.selectList(offset, size);
            total = artistMapper.countTotal();
        }

        List<ArtistVO> voList = artists.stream()
                .map(ArtistVO::fromEntity)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("records", voList);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArtistVO updateArtist(Artist artist) {
        // 校验艺人是否存在
        Artist existing = artistMapper.selectById(artist.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.ARTIST_NOT_FOUND);
        }
        // 只更新非空字段（name / avatar / bio / country）
        artistMapper.update(artist);
        log.info("管理员更新艺人信息: id={}, name={}, avatar={}", artist.getId(), artist.getName(), artist.getAvatar());
        return ArtistVO.fromEntity(artistMapper.selectById(artist.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteArtist(Long adminId, Long artistId) {
        Artist artist = artistMapper.selectById(artistId);
        if (artist == null) {
            throw new BusinessException(ErrorCode.ARTIST_NOT_FOUND);
        }
        artistMapper.deleteById(artistId);
        log.info("管理员 {} 删除艺人 {}: {}", adminId, artistId, artist.getName());
    }

    // ==================== 评论管理 ====================

    @Override
    public Map<String, Object> listComments(String targetType, int page, int size) {
        int offset = (page - 1) * size;

        List<Comment> comments = commentMapper.selectAllForAdmin(offset, size, targetType);
        long total = commentMapper.countAllForAdmin(targetType);

        Map<String, Object> result = new HashMap<>();
        result.put("records", comments);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long adminId, Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ErrorCode.ADMIN_COMMENT_NOT_FOUND);
        }
        commentMapper.adminDelete(commentId);
        log.info("管理员 {} 删除评论 {}: {}", adminId, commentId, comment.getContent());
    }

    // ==================== 批量导入 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchImportSongs(Long adminId, MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, String>> errors = new ArrayList<>();
        int success = 0;
        int failed = 0;

        // 1. Validate file type — check magic bytes for ZIP (PK\x03\x04)
        byte[] header = new byte[4];
        try (InputStream is = file.getInputStream()) {
            int read = is.read(header);
            if (read < 4 || header[0] != 0x50 || header[1] != 0x4B
                    || header[2] != 0x03 || header[3] != 0x04) {
                throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "仅支持 ZIP 格式的批量导入");
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "文件读取失败");
        }

        // 2. Create temp directory for extraction
        File tempDir = new File(FileUtil.getTmpDirPath(),
                "batch_import_" + System.currentTimeMillis());
        FileUtil.mkdir(tempDir);

        try {
            // 3. Extract ZIP entries to temp directory
            try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (!entry.isDirectory()) {
                        File outFile = new File(tempDir, entry.getName());
                        FileUtil.mkParentDirs(outFile);
                        try (FileOutputStream fos = new FileOutputStream(outFile)) {
                            byte[] buffer = new byte[8192];
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                        }
                    }
                    zis.closeEntry();
                }
            }

            // 4. List extracted files and group by base name
            File[] allFiles = tempDir.listFiles();
            if (allFiles == null || allFiles.length == 0) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "ZIP 文件为空");
            }

            // Group files by base name (filename without extension)
            Map<String, List<File>> groups = new HashMap<>();
            for (File f : allFiles) {
                String name = f.getName();
                int dotIdx = name.lastIndexOf('.');
                if (dotIdx > 0) {
                    String base = name.substring(0, dotIdx).toLowerCase();
                    groups.computeIfAbsent(base, k -> new ArrayList<>()).add(f);
                }
            }

            // 5. Process each group as a song
            // Filename format: 歌曲名_歌手名.mp3 (e.g. 夜曲_周杰伦.mp3)
            // Matching .lrc file with same base name is paired automatically
            for (Map.Entry<String, List<File>> group : groups.entrySet()) {
                String baseName = group.getKey();
                List<File> files = group.getValue();

                try {
                    File audioFile = null;
                    File lyricFile = null;
                    File coverFile = null;

                    for (File f : files) {
                        String name = f.getName().toLowerCase();
                        if (name.endsWith(".mp3") || name.endsWith(".flac")
                                || name.endsWith(".wav") || name.endsWith(".ogg")) {
                            audioFile = f;
                        } else if (name.endsWith(".lrc") || name.endsWith(".txt")) {
                            lyricFile = f;
                        } else if (name.startsWith("cover")
                                && (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png"))) {
                            coverFile = f;
                        }
                    }

                    if (audioFile == null) {
                        failed++;
                        Map<String, String> err = new HashMap<>();
                        err.put("file", baseName);
                        err.put("reason", "未找到音频文件（支持 mp3/flac/wav/ogg）");
                        errors.add(err);
                        continue;
                    }

                    // Parse title and artist from filename (format: 歌曲名_歌手名)
                    String fileName = audioFile.getName();
                    String fileBaseName = fileName.substring(0, fileName.lastIndexOf('.'));
                    String songTitle = fileBaseName;
                    String artistName = null;

                    // Split by last underscore to extract artist name
                    int lastUnderscore = fileBaseName.lastIndexOf('_');
                    if (lastUnderscore > 0 && lastUnderscore < fileBaseName.length() - 1) {
                        songTitle = fileBaseName.substring(0, lastUnderscore).trim();
                        artistName = fileBaseName.substring(lastUnderscore + 1).trim();
                    }

                    // Find or create artist by name
                    Long artistId = null;
                    if (artistName != null && !artistName.isEmpty()) {
                        Artist existing = artistMapper.selectByName(artistName);
                        if (existing != null) {
                            artistId = existing.getId();
                        } else {
                            Artist newArtist = new Artist();
                            newArtist.setName(artistName);
                            artistMapper.insert(newArtist);
                            artistId = newArtist.getId();
                            log.info("批量导入自动创建艺人: id={}, name={}", artistId, artistName);
                        }
                    }

                    // Upload audio file via FileService
                    String audioUrl = uploadFile(audioFile, "audio");

                    // Upload cover if exists
                    String coverUrl = null;
                    if (coverFile != null) {
                        coverUrl = uploadFile(coverFile, "cover");
                    }

                    // Upload lyric if exists
                    String lyricUrl = null;
                    if (lyricFile != null) {
                        lyricUrl = uploadFile(lyricFile, "lyric");
                    }

                    // Create song record — status ACTIVE means no admin verification needed
                    Song song = new Song();
                    song.setTitle(songTitle);
                    song.setArtistId(artistId);
                    song.setDuration(0);
                    song.setAudioUrl(audioUrl);
                    song.setCoverUrl(coverUrl);
                    song.setLyricUrl(lyricUrl);
                    song.setStatus("ACTIVE");
                    song.setPlayCount(0L);
                    song.setUploaderId(adminId);

                    songMapper.insert(song);
                    success++;
                    log.info("批量导入歌曲成功: title={}, artist={}, audio={}", songTitle, artistName, audioUrl);
                } catch (Exception e) {
                    log.error("批量导入处理失败: baseName={}", baseName, e);
                    failed++;
                    Map<String, String> err = new HashMap<>();
                    err.put("file", baseName);
                    err.put("reason", e.getMessage());
                    errors.add(err);
                }
            }

            result.put("total", success + failed);
            result.put("success", success);
            result.put("failed", failed);
            result.put("errors", errors);
            return result;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "ZIP 解压失败: " + e.getMessage());
        } finally {
            // Cleanup temp directory
            FileUtil.del(tempDir);
        }
    }

    /**
     * 上传文件（将 java.io.File 包装为 MultipartFile 后调用 FileService）
     */
    private String uploadFile(File file, String type) {
        try {
            FileMultipartFile multipartFile = new FileMultipartFile(file);
            return fileService.upload(multipartFile, type, null);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED,
                    "文件上传失败: " + file.getName(), e);
        }
    }

    /**
     * 将 java.io.File 适配为 Spring MultipartFile
     * <p>
     * 用于将 ZIP 中解压出的文件传递给 FileService 进行持久化存储和验证。
     */
    private static class FileMultipartFile implements MultipartFile {

        private final File file;
        private final String contentType;
        private final byte[] cachedBytes;

        FileMultipartFile(File file) throws IOException {
            this.file = file;
            this.cachedBytes = FileUtil.readBytes(file);
            String name = file.getName().toLowerCase();
            if (name.endsWith(".mp3")) {
                this.contentType = "audio/mpeg";
            } else if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
                this.contentType = "image/jpeg";
            } else if (name.endsWith(".png")) {
                this.contentType = "image/png";
            } else if (name.endsWith(".lrc") || name.endsWith(".txt")) {
                this.contentType = "text/plain";
            } else {
                this.contentType = "application/octet-stream";
            }
        }

        @Override
        public String getName() {
            return file.getName();
        }

        @Override
        public String getOriginalFilename() {
            return file.getName();
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return cachedBytes.length == 0;
        }

        @Override
        public long getSize() {
            return cachedBytes.length;
        }

        @Override
        public byte[] getBytes() {
            return cachedBytes;
        }

        @Override
        public InputStream getInputStream() {
            return new java.io.ByteArrayInputStream(cachedBytes);
        }

        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {
            FileUtil.writeBytes(cachedBytes, dest);
        }

        @Override
        public Resource getResource() {
            return new FileSystemResource(file);
        }
    }
}
