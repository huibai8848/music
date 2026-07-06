package com.example.music.service.impl;

import cn.hutool.core.util.StrUtil;
import com.example.music.constant.ErrorCode;
import com.example.music.constant.RedisKeys;
import com.example.music.entity.Album;
import com.example.music.entity.Artist;
import com.example.music.entity.Song;
import com.example.music.entity.SongCategory;
import com.example.music.entity.User;
import com.example.music.exception.BusinessException;
import com.example.music.mapper.AlbumMapper;
import com.example.music.mapper.ArtistMapper;
import com.example.music.mapper.SongCategoryMapper;
import com.example.music.mapper.SongMapper;
import com.example.music.mapper.UserMapper;
import com.example.music.service.NotificationService;
import com.example.music.service.SongService;
import com.example.music.utils.CacheUtil;
import com.example.music.vo.SongVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 歌曲服务实现
 *
 * 核心功能：
 * 1. 歌曲 CRUD + 搜索（关联艺人/专辑名）
 * 2. LRC 歌词解析与缓存
 * 3. 播放量计数缓冲（Redis INCR → 定时刷入 DB）
 * 4. 搜索缓存（TTL 60s）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SongServiceImpl implements SongService {

    private final SongMapper songMapper;
    private final ArtistMapper artistMapper;
    private final AlbumMapper albumMapper;
    private final SongCategoryMapper songCategoryMapper;
    private final com.example.music.mapper.LikesMapper likesMapper;
    private final com.example.music.mapper.FavoriteMapper favoriteMapper;
    private final CacheUtil cacheUtil;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    /** 文件上传根路径（从配置注入，用于歌词路径转换） */
    @Value("${music.file.upload-dir:data/music}")
    private String uploadDir;

    @PostConstruct
    public void initUploadDir() {
        java.io.File dir = new java.io.File(uploadDir);
        if (!dir.isAbsolute()) {
            uploadDir = new java.io.File(System.getProperty("user.dir"), uploadDir).getAbsolutePath();
        }
    }

    /**
     * LRC 时间戳正则：匹配 [mm:ss.xx] 或 [mm:ss]
     * 用于解析一行中所有的 timestamps（同一行可能多个时间戳共享一句歌词）
     */
    private static final Pattern LRC_TIME_PATTERN = Pattern.compile("\\[(\\d{2}):(\\d{2})(?:\\.(\\d{2,3}))?\\]");

    /**
     * LRC 元信息正则：匹配 [ti:xxx]、[ar:xxx]、[al:xxx] 等标签行
     */
    private static final Pattern LRC_META_PATTERN = Pattern.compile("\\[(\\w+):([^\\]]*)\\]");

    // ==================== 查询 ====================

    @Override
    public SongVO getSongDetail(Long id) {
        Song song = songMapper.selectById(id);
        if (song == null) {
            throw new BusinessException(ErrorCode.SONG_NOT_FOUND);
        }
        return enrichSongVO(song);
    }

    @Override
    public List<SongVO> listSongs(int page, int size, String genre, String language, Integer releaseYear, Long artistId, Long albumId, String status) {
        int offset = (page - 1) * size;
        String queryStatus = (status != null && !status.isEmpty()) ? status : "ACTIVE";
        return songMapper.selectList(offset, size, genre, language, releaseYear, artistId, albumId, queryStatus).stream()
                .map(this::enrichSongVO)
                .collect(Collectors.toList());
    }

    @Override
    public long countSongs(String genre, String language, Integer releaseYear, Long artistId, Long albumId, String status) {
        String queryStatus = (status != null && !status.isEmpty()) ? status : "ACTIVE";
        return songMapper.countTotal(genre, language, releaseYear, artistId, albumId, queryStatus);
    }

    @Override
    public List<SongVO> searchSongs(String keyword, int page, int size) {
        if (StrUtil.isBlank(keyword)) {
            return Collections.emptyList();
        }
        int offset = (page - 1) * size;
        try {
            return songMapper.search(keyword, offset, size).stream()
                    .map(this::enrichSongVO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("歌曲搜索查询失败, keyword={}, page={}, size={}", keyword, page, size, e);
            return Collections.emptyList();
        }
    }

    @Override
    public long countSearch(String keyword) {
        if (StrUtil.isBlank(keyword)) return 0;
        try {
            return songMapper.countSearch(keyword);
        } catch (Exception e) {
            log.error("歌曲搜索计数查询失败, keyword={}", keyword, e);
            return 0;
        }
    }

    @Override
    public List<SongVO> getHotSongs(int limit) {
        // 尝试从缓存获取
        String cacheKey = RedisKeys.CACHE_HOT_SONGS;
        List<SongVO> cached = cacheUtil.get(cacheKey, List.class);
        if (cached != null) {
            return cached;
        }

        // 缓存未命中，从数据库查询
        List<SongVO> hotSongs = songMapper.selectHot(limit).stream()
                .map(this::enrichSongVO)
                .collect(Collectors.toList());

        // 写入缓存（TTL 300s）
        cacheUtil.set(cacheKey, hotSongs, RedisKeys.TTL_HOT_SONGS, TimeUnit.SECONDS);
        return hotSongs;
    }

    @Override
    public List<SongVO> getSongsByArtist(Long artistId) {
        return songMapper.selectByArtistId(artistId).stream()
                .map(this::enrichSongVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<SongVO> getSongsByUploader(Long uploaderId) {
        return songMapper.selectByUploaderId(uploaderId).stream()
                .map(this::enrichSongVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<SongVO> getSongsByAlbum(Long albumId) {
        return songMapper.selectByAlbumId(albumId).stream()
                .map(this::enrichSongVO)
                .collect(Collectors.toList());
    }

    // ==================== LRC 歌词 ====================

    @Override
    public String getLyrics(Long songId) {
        // 1. 尝试从缓存获取
        String cacheKey = RedisKeys.CACHE_LYRICS + songId;
        String cached = cacheUtil.get(cacheKey, String.class);
        if (cached != null) {
            return cached;
        }

        // 2. 查询数据库
        Song song = songMapper.selectById(songId);
        if (song == null) {
            throw new BusinessException(ErrorCode.SONG_NOT_FOUND);
        }

        // 3. 如果已有解析后的歌词 JSON，直接返回
        if (StrUtil.isNotBlank(song.getLyrics())) {
            cacheUtil.set(cacheKey, song.getLyrics(), RedisKeys.TTL_LYRICS, TimeUnit.SECONDS);
            return song.getLyrics();
        }

        // 4. 如果只有 LRC 文件路径，解析后返回
        if (StrUtil.isNotBlank(song.getLyricUrl())) {
            String lyricsJson = parseLrcFile(song.getLyricUrl());
            if (lyricsJson != null) {
                // 回写到数据库
                song.setLyrics(lyricsJson);
                songMapper.update(song);
                // 写入缓存
                cacheUtil.set(cacheKey, lyricsJson, RedisKeys.TTL_LYRICS, TimeUnit.SECONDS);
                return lyricsJson;
            }
        }

        // 5. 没有歌词
        return "[]";
    }

    // ==================== 播放量统计 ====================

    /**
     * 播放量上报
     * <p>
     * 已废弃：播放量统计统一由 PlayHistoryService.recordPlay() 处理
     * （插入 play_history 表 + 递增 song.play_count）。
     * 此方法保留空实现避免前端调用报错。
     */
    @Override
    public void reportPlay(Long songId) {
        // 播放量统计已迁移至 PlayHistoryService.recordPlay()
        log.debug("播放量上报（空实现，已迁移至 PlayHistoryService）: songId={}", songId);
    }

    // ==================== 增删改 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SongVO createSong(Song song) {
        if (StrUtil.isBlank(song.getTitle())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "歌曲名称不能为空");
        }
        if (song.getArtistId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "必须指定艺人");
        }
        if (StrUtil.isBlank(song.getAudioUrl())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "音频文件不能为空");
        }
        // 设置默认状态：上传时默认 PENDING，需管理员审核通过后才展示
        if (StrUtil.isBlank(song.getStatus())) {
            song.setStatus("PENDING");
        }
        song.setPlayCount(0L);
        songMapper.insert(song);

        // 保存歌曲分类关联
        if (song.getCategoryIds() != null && !song.getCategoryIds().isEmpty()) {
            List<SongCategory> scList = song.getCategoryIds().stream()
                    .map(catId -> {
                        SongCategory sc = new SongCategory();
                        sc.setSongId(song.getId());
                        sc.setCategoryId(catId);
                        return sc;
                    })
                    .collect(Collectors.toList());
            songCategoryMapper.insertBatch(scList);
        }

        log.info("新增歌曲: id={}, title={}", song.getId(), song.getTitle());
        return enrichSongVO(song);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SongVO updateSong(Song song) {
        Song existing = songMapper.selectById(song.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.SONG_NOT_FOUND);
        }
        songMapper.update(song);
        // 清除热门歌曲缓存，确保下次获取时是更新后的数据
        cacheUtil.delete(RedisKeys.CACHE_HOT_SONGS);
        return enrichSongVO(songMapper.selectById(song.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSong(Long id) {
        if (songMapper.selectById(id) == null) {
            throw new BusinessException(ErrorCode.SONG_NOT_FOUND);
        }
        songMapper.deleteById(id);
        // 清除相关缓存
        cacheUtil.delete(RedisKeys.CACHE_HOT_SONGS);
        cacheUtil.delete(RedisKeys.CACHE_LYRICS + id);
        log.info("删除歌曲: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditSong(Long id, String status) {
        Song song = songMapper.selectById(id);
        if (song == null) {
            throw new BusinessException(ErrorCode.SONG_NOT_FOUND);
        }
        if (!Arrays.asList("ACTIVE", "REJECTED").contains(status.toUpperCase())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "审核状态无效");
        }
        songMapper.updateStatus(id, status.toUpperCase());
        // 审核后清除热门歌曲缓存，确保新上线的歌曲能被展示
        cacheUtil.delete(RedisKeys.CACHE_HOT_SONGS);
        log.info("审核歌曲: id={}, status={}", id, status);
    }

    // ==================== 歌曲分类管理 ====================

    @Override
    public List<Long> getSongCategoryIds(Long songId) {
        return songCategoryMapper.selectCategoryIdsBySongId(songId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSongCategories(Long songId, List<Long> categoryIds) {
        // 先验证歌曲存在
        Song song = songMapper.selectById(songId);
        if (song == null) {
            throw new BusinessException(ErrorCode.SONG_NOT_FOUND);
        }
        // 删除旧关联
        songCategoryMapper.deleteBySongId(songId);
        // 插入新关联
        if (categoryIds != null && !categoryIds.isEmpty()) {
            List<SongCategory> list = categoryIds.stream()
                    .map(catId -> {
                        SongCategory sc = new SongCategory();
                        sc.setSongId(songId);
                        sc.setCategoryId(catId);
                        return sc;
                    })
                    .collect(Collectors.toList());
            songCategoryMapper.insertBatch(list);
        }
    }

    // ==================== 私有方法 ====================

    // ==================== 会员提交歌曲审核 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SongVO submitSongForReview(Song song, Artist artist, Album album) {
        // 1. 解析/创建艺人
        Long artistId = resolveArtistForSubmit(artist);
        song.setArtistId(artistId);

        // 2. 解析/创建专辑
        if (album != null) {
            album.setArtistId(artistId);
            Long albumId = resolveAlbumForSubmit(album);
            song.setAlbumId(albumId);
        }

        // 3. 创建歌曲
        if (StrUtil.isBlank(song.getStatus())) {
            song.setStatus("PENDING");
        }
        song.setPlayCount(0L);
        songMapper.insert(song);

        // 4. 保存歌曲分类关联
        if (song.getCategoryIds() != null && !song.getCategoryIds().isEmpty()) {
            List<SongCategory> scList = song.getCategoryIds().stream()
                    .map(catId -> {
                        SongCategory sc = new SongCategory();
                        sc.setSongId(song.getId());
                        sc.setCategoryId(catId);
                        return sc;
                    })
                    .collect(Collectors.toList());
            songCategoryMapper.insertBatch(scList);
        }

        // 5. 如果提供了 LRC 歌词文件，立即解析并回写 lyrics 字段
        if (StrUtil.isNotBlank(song.getLyricUrl())) {
            String lyricsJson = parseLrcFile(song.getLyricUrl());
            if (lyricsJson != null) {
                song.setLyrics(lyricsJson);
                songMapper.update(song);
            }
        }

        log.info("歌曲提审成功: id={}, title={}, artistId={}", song.getId(), song.getTitle(), artistId);

        // 6. 通知上传者（歌曲正在审核）+ 通知所有管理员审核新歌曲 —— 延迟到事务提交后执行
        //    如果歌曲已经是 ACTIVE（管理员上传），跳过所有通知
        Long songId = song.getId();
        String songTitle = song.getTitle();
        Long uploaderId = song.getUploaderId();
        boolean needsReview = !"ACTIVE".equals(song.getStatus());
        if (needsReview) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    // 6a. 通知上传者：歌曲正在审核
                    if (uploaderId != null) {
                        try {
                            notificationService.createNotification(
                                    uploaderId,
                                    "SONG_REVIEW",
                                    "歌曲审核中",
                                    "您上传的歌曲《" + songTitle + "》已提交，正在审核中，请耐心等待。",
                                    "SONG",
                                    songId
                            );
                        } catch (Exception e) {
                            log.error("通知上传者审核中失败: songId={}, uploaderId={}", songId, uploaderId, e);
                        }
                    }

                    // 6b. 通知所有管理员审核新歌曲
                    notifyAdminsForReview(songId, songTitle);
                }
            });
        }

        return enrichSongVO(song);
    }

    /**
     * 通知所有管理员有新歌曲等待审核
     */
    private void notifyAdminsForReview(Long songId, String songTitle) {
        try {
            List<User> admins = userMapper.selectByRole("ADMIN");
            if (admins == null || admins.isEmpty()) {
                log.debug("没有管理员账号，跳过审核通知");
                return;
            }
            for (User admin : admins) {
                notificationService.createNotification(
                        admin.getId(),
                        "SONG_REVIEW",
                        "新歌曲待审核",
                        "会员上传了新歌曲《" + songTitle + "》，请前往审核",
                        "SONG",
                        songId
                );
            }
            log.info("已通知 {} 位管理员审核歌曲: id={}, title={}", admins.size(), songId, songTitle);
        } catch (Exception e) {
            log.error("通知管理员审核歌曲失败: id={}, title={}", songId, songTitle, e);
        }
    }

    /**
     * 在事务中解析或创建艺人
     * <p>
     * 优先使用已有 ID，否则按名称查找或新建。
     * 所有 MyBatis SQL 均使用 #{ } 预编译，杜绝 SQL 注入风险。
     */
    private Long resolveArtistForSubmit(Artist artist) {
        if (artist == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "必须指定艺人");
        }
        // 优先使用已有 ID
        if (artist.getId() != null) {
            Artist existing = artistMapper.selectById(artist.getId());
            if (existing != null) {
                return existing.getId();
            }
            throw new BusinessException(ErrorCode.BAD_REQUEST, "指定的艺人不存在");
        }
        // 按名称查找或新建（防止空格/引号注入：MyBatis #{ } 预编译处理）
        if (StrUtil.isBlank(artist.getName())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "艺人名称不能为空");
        }
        Artist match = artistMapper.selectByName(artist.getName().trim());
        if (match != null) {
            return match.getId();
        }
        // 新建艺人
        artist.setName(artist.getName().trim());
        artistMapper.insert(artist);
        log.info("新建艺人: id={}, name={}", artist.getId(), artist.getName());
        return artist.getId();
    }

    /**
     * 在事务中解析或创建专辑
     * <p>
     * 优先使用已有 ID，否则按标题+艺人查找或新建。
     * 所有 MyBatis SQL 均使用 #{ } 预编译，杜绝 SQL 注入风险。
     */
    private Long resolveAlbumForSubmit(Album album) {
        // 优先使用已有 ID
        if (album.getId() != null) {
            Album existing = albumMapper.selectById(album.getId());
            if (existing != null) {
                return existing.getId();
            }
            return null; // ID 无效时不报错
        }
        // 按标题+艺人查找或新建
        if (StrUtil.isBlank(album.getTitle())) {
            return null;
        }
        String title = album.getTitle().trim();
        Album match = albumMapper.selectByTitleAndArtist(title, album.getArtistId());
        if (match != null) {
            return match.getId();
        }
        // 新建专辑
        album.setTitle(title);
        albumMapper.insert(album);
        log.info("新建专辑: id={}, title={}", album.getId(), album.getTitle());
        return album.getId();
    }

    /**
     * 丰富 SongVO：填充艺人名、专辑名、分类 ID 列表、喜欢/收藏数
     * <p>
     * 注意：如果歌曲状态不是 ACTIVE（审核中/已驳回），则不填充艺人名和专辑名，
     * 避免审核中的歌曲暴露关联的艺人/专辑信息。
     */
    /**
     * 丰富 SongVO：填充艺人名、专辑名、分类 ID 列表、喜欢/收藏数
     * <p>
     * 每个数据库查询单独包裹 try-catch，防止单个歌曲的关联数据异常
     * （如艺人/专辑被物理删除、分类/点赞表数据损坏）导致整个搜索结果返回 500。
     * 查询失败的字段保留默认值（null/0），不影响其他字段和其他歌曲。
     */
    private SongVO enrichSongVO(Song song) {
        if (song == null) return null;
        SongVO vo = SongVO.fromEntity(song);

        // 仅 ACTIVE 状态的歌曲才填充艺人名和专辑名
        boolean isActive = "ACTIVE".equals(song.getStatus());
        Long songId = song.getId();

        // — 以下每个 Mapper 调用均独立 try-catch，单项失败不影响整体 —

        // 1. 填充艺人名
        if (isActive && song.getArtistId() != null) {
            try {
                var artist = artistMapper.selectById(song.getArtistId());
                if (artist != null) {
                    vo.setArtistName(artist.getName());
                }
            } catch (Exception e) {
                log.warn("填充艺人名失败, songId={}, artistId={}", songId, song.getArtistId(), e);
            }
        }

        // 2. 填充专辑名
        if (isActive && song.getAlbumId() != null) {
            try {
                var album = albumMapper.selectById(song.getAlbumId());
                if (album != null) {
                    vo.setAlbumTitle(album.getTitle());
                }
            } catch (Exception e) {
                log.warn("填充专辑名失败, songId={}, albumId={}", songId, song.getAlbumId(), e);
            }
        }

        // 3. 填充分类 ID 列表
        if (songId != null) {
            try {
                vo.setCategoryIds(songCategoryMapper.selectCategoryIdsBySongId(songId));
            } catch (Exception e) {
                log.warn("填充分类 ID 列表失败, songId={}", songId, e);
            }
        }

        // 4. 填充喜欢数 / 收藏数
        if (songId != null) {
            try {
                vo.setLikeCount(likesMapper.countByTarget("SONG", songId));
            } catch (Exception e) {
                log.warn("填充喜欢数失败, songId={}", songId, e);
            }
            try {
                vo.setFavoriteCount(favoriteMapper.countByTarget("SONG", songId));
            } catch (Exception e) {
                log.warn("填充收藏数失败, songId={}", songId, e);
            }
        }

        // 5. 播放量直接使用 song.play_count 字段（已在播放时同步更新）
        //    不再叠加 Redis 缓冲，详见 PlayHistoryService.recordPlay()

        return vo;
    }

    /**
     * 解析 LRC 歌词文件为 JSON 格式
     * <p>
     * 支持一行多个时间戳（如 {@code [00:01.00][00:05.00]歌词}），
     * 会为每个时间戳生成一条独立记录。
     * 过滤 [ti:]、[ar:] 等元信息标签行。
     *
     * @param filePath LRC 文件路径
     * @return JSON 字符串：[{time: 120.5, text: "歌词"}, ...]，解析失败返回 null
     */
    private String parseLrcFile(String filePath) {
        List<Map<String, Object>> lyricsList = new ArrayList<>();

        // 将 URL 路径（/api/files/lyric/...）转换为实际文件系统路径
        String fsPath = filePath;
        if (fsPath.startsWith("/api/files/")) {
            fsPath = uploadDir + fsPath.substring("/api/files".length());
        }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(fsPath), StandardCharsets.UTF_8))) {

            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // 跳过元信息行：如 [ti:标题]、[ar:艺人]、[al:专辑]、[by:编辑]、[offset:偏移]
                if (LRC_META_PATTERN.matcher(line).matches()) continue;

                // 找出行中所有时间戳
                Matcher timeMatcher = LRC_TIME_PATTERN.matcher(line);
                List<Double> timestamps = new ArrayList<>();
                int lastEnd = 0;
                while (timeMatcher.find()) {
                    int minutes = Integer.parseInt(timeMatcher.group(1));
                    int seconds = Integer.parseInt(timeMatcher.group(2));
                    String millisStr = timeMatcher.group(3);

                    // 计算总秒数（保留两位小数）
                    double totalSeconds = minutes * 60.0 + seconds;
                    if (millisStr != null) {
                        totalSeconds += millisStr.length() == 2
                                ? Integer.parseInt(millisStr) / 100.0
                                : Integer.parseInt(millisStr) / 1000.0;
                    }
                    totalSeconds = Math.round(totalSeconds * 100.0) / 100.0;
                    timestamps.add(totalSeconds);
                    lastEnd = timeMatcher.end(); // 记录最后一个时间戳的结束位置
                }

                // 如果没有时间戳则跳过
                if (timestamps.isEmpty()) continue;

                // 提取歌词文本（最后一个 ] 之后的内容）
                String text = line.substring(lastEnd).trim();
                if (text.isEmpty()) continue;

                // 为每个时间戳生成一条歌词记录
                for (Double time : timestamps) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("time", time);
                    item.put("text", text);
                    lyricsList.add(item);
                }
            }
        } catch (Exception e) {
            log.warn("LRC 文件解析失败: path={}, error={}", filePath, e.getMessage());
            return null;
        }

        // 按时间排序
        lyricsList.sort(Comparator.comparingDouble(m -> (Double) m.get("time")));

        // 转换为 JSON 字符串
        try {
            return com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                    .writeValueAsString(lyricsList);
        } catch (Exception e) {
            log.warn("歌词 JSON 序列化失败", e);
            return null;
        }
    }
}