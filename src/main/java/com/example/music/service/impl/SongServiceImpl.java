package com.example.music.service.impl;

import cn.hutool.core.util.StrUtil;
import com.example.music.constant.ErrorCode;
import com.example.music.constant.RedisKeys;
import com.example.music.entity.Album;
import com.example.music.entity.Artist;
import com.example.music.entity.Song;
import com.example.music.entity.SongCategory;
import com.example.music.exception.BusinessException;
import com.example.music.mapper.AlbumMapper;
import com.example.music.mapper.ArtistMapper;
import com.example.music.mapper.SongCategoryMapper;
import com.example.music.mapper.SongMapper;
import com.example.music.service.SongService;
import com.example.music.utils.CacheUtil;
import com.example.music.vo.SongVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public List<SongVO> listSongs(int page, int size, String genre, String status) {
        int offset = (page - 1) * size;
        // 默认只查 ACTIVE 状态的歌曲
        String queryStatus = (status != null && !status.isEmpty()) ? status : "ACTIVE";
        return songMapper.selectList(offset, size, genre, queryStatus).stream()
                .map(this::enrichSongVO)
                .collect(Collectors.toList());
    }

    @Override
    public long countSongs(String genre, String status) {
        String queryStatus = (status != null && !status.isEmpty()) ? status : "ACTIVE";
        return songMapper.countTotal(genre, queryStatus);
    }

    @Override
    public List<SongVO> searchSongs(String keyword, int page, int size) {
        if (StrUtil.isBlank(keyword)) {
            return Collections.emptyList();
        }

        int offset = (page - 1) * size;
        return songMapper.search(keyword, offset, size).stream()
                .map(this::enrichSongVO)
                .collect(Collectors.toList());
    }

    @Override
    public long countSearch(String keyword) {
        if (StrUtil.isBlank(keyword)) return 0;
        return songMapper.countSearch(keyword);
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

    @Override
    public void reportPlay(Long songId) {
        // 使用 Redis INCR 原子递增播放计数缓冲
        String bufferKey = RedisKeys.PLAY_COUNT_BUFFER + songId;
        cacheUtil.increment(bufferKey, 1);

        // 更新排行榜 ZSet（原子自增，用于日榜/周榜/月榜/总榜）
        java.time.LocalDate now = java.time.LocalDate.now();
        String today = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        cacheUtil.zIncrementScore(RedisKeys.RANKING_DAILY + today, songId.toString(), 1);

        String week = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyy'w'ww"));
        cacheUtil.zIncrementScore(RedisKeys.RANKING_WEEKLY + week, songId.toString(), 1);

        String month = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
        cacheUtil.zIncrementScore(RedisKeys.RANKING_MONTHLY + month, songId.toString(), 1);

        cacheUtil.zIncrementScore(RedisKeys.RANKING_ALL, songId.toString(), 1);

        log.debug("播放量上报: songId={}", songId);
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

        // 4. 如果提供了 LRC 歌词文件，立即解析并回写 lyrics 字段
        if (StrUtil.isNotBlank(song.getLyricUrl())) {
            String lyricsJson = parseLrcFile(song.getLyricUrl());
            if (lyricsJson != null) {
                song.setLyrics(lyricsJson);
                songMapper.update(song);
            }
        }

        log.info("歌曲提审成功: id={}, title={}, artistId={}", song.getId(), song.getTitle(), artistId);
        return enrichSongVO(song);
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
     */
    private SongVO enrichSongVO(Song song) {
        if (song == null) return null;
        SongVO vo = SongVO.fromEntity(song);

        // 填充艺人名
        if (song.getArtistId() != null) {
            var artist = artistMapper.selectById(song.getArtistId());
            if (artist != null) {
                vo.setArtistName(artist.getName());
            }
        }

        // 填充专辑名
        if (song.getAlbumId() != null) {
            var album = albumMapper.selectById(song.getAlbumId());
            if (album != null) {
                vo.setAlbumTitle(album.getTitle());
            }
        }

        // 填充分类 ID 列表
        vo.setCategoryIds(songCategoryMapper.selectCategoryIdsBySongId(song.getId()));

        // 填充喜欢数 / 收藏数
        Long songId = song.getId();
        if (songId != null) {
            vo.setLikeCount(likesMapper.countByTarget("SONG", songId));
            vo.setFavoriteCount(favoriteMapper.countByTarget("SONG", songId));
        }

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

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {

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

                    // 计算总秒数（保留一位小数）
                    double totalSeconds = minutes * 60.0 + seconds;
                    if (millisStr != null) {
                        totalSeconds += millisStr.length() == 2
                                ? Integer.parseInt(millisStr) / 100.0
                                : Integer.parseInt(millisStr) / 1000.0;
                    }
                    totalSeconds = Math.round(totalSeconds * 10.0) / 10.0;
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