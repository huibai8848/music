package com.example.music.controller;

import cn.hutool.core.util.StrUtil;
import com.example.music.constant.ErrorCode;
import com.example.music.constant.RedisKeys;
import com.example.music.dto.SongUploadDTO;
import cn.hutool.core.util.StrUtil;
import com.example.music.constant.ErrorCode;
import com.example.music.constant.RedisKeys;
import com.example.music.dto.SongUploadDTO;
import com.example.music.entity.Album;
import com.example.music.entity.Artist;
import com.example.music.entity.Category;
import com.example.music.entity.Song;
import com.example.music.entity.SystemConfig;
import com.example.music.exception.BusinessException;
import com.example.music.mapper.CategoryMapper;
import com.example.music.mapper.SystemConfigMapper;
import com.example.music.service.FileService;
import com.example.music.service.SongService;
import com.example.music.utils.AudioUtils;
import com.example.music.utils.CacheUtil;
import com.example.music.utils.RequestContext;
import com.example.music.vo.R;
import com.example.music.vo.SongVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 会员控制器
 * <p>
 * 提供会员专属功能：上传音乐、个人音乐库等。
 * 需要 VIP 或 ADMIN 角色才能访问。
 * <p>
 * 上传次数限制来源于 {@code system_config} 表的 {@code member_upload_limit} 配置项，
 * 由 Redis 缓存减少数据库查询，默认值 5。
 */
@Slf4j
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final SongService songService;
    private final FileService fileService;
    private final CacheUtil cacheUtil;
    private final ObjectMapper objectMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final CategoryMapper categoryMapper;

    /** 文件上传根路径（从配置注入，用于从 audioUrl 反查文件系统路径） */
    @Value("${music.file.upload-dir:data/music}")
    private String uploadDir;

    @PostConstruct
    public void initUploadDir() {
        java.io.File dir = new java.io.File(uploadDir);
        if (!dir.isAbsolute()) {
            uploadDir = new java.io.File(System.getProperty("user.dir"), uploadDir).getAbsolutePath();
        }
    }

    /** 会员存储空间上限（500MB） */
    private static final long MEMBER_STORAGE_LIMIT = 500L * 1024 * 1024;

    /** 系统配置缓存 TTL（秒） */
    private static final long CONFIG_CACHE_TTL = 60;

    /** 系统配置：会员每日上传次数限制的键名 */
    private static final String CONFIG_KEY_UPLOAD_LIMIT = "member_upload_limit";

    /** 系统配置：默认上传次数（Redis/DB 同时不可用时回退） */
    private static final int DEFAULT_UPLOAD_LIMIT = 5;

    /**
     * 会员上传音乐（文件+元数据一次性提交）
     * <p>
     * 使用 multipart/form-data 同时上传音频、封面、歌词文件和歌曲元数据。
     * 事务边界在 {@link SongService#submitSongForReview(Song, Artist, Album)} 中，
     * 确保艺人/专辑/歌曲的 DB 操作原子性。
     *
     * <pre>
     * 请求参数：
     *   audio    – 音频文件（必填，MP3/AAC，≤50MB）
     *   cover    – 封面图（可选，JPG/PNG，≤5MB）
     *   lyric    – 歌词文件（可选，LRC/TXT，≤100KB）
     *   songInfo – 歌曲元数据（JSON 字符串，必填）
     *
     * songInfo JSON 字段：
     *   title       – 歌曲名（必填）
     *   artistId    – 艺人 ID（与 artistName 二选一）
     *   artistName  – 艺人名（新建艺人时使用）
     *   albumId     – 专辑 ID（与 albumTitle 二选一）
     *   albumTitle  – 专辑名（新建专辑时使用）
     *   genre       – 风格（可选）
     *   language    – 语种（可选）
     *   releaseYear – 发行年份（可选）
     * </pre>
     *
     * @return 包含歌曲 VO 和剩余上传次数的 Map
     */
    @PostMapping(value = "/upload-music", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<Map<String, Object>> uploadMusicWithFiles(
            @RequestPart("audio") MultipartFile audioFile,
            @RequestPart(value = "cover", required = false) MultipartFile coverFile,
            @RequestPart(value = "lyric", required = false) MultipartFile lyricFile,
            @RequestParam("songInfo") String songInfoJson) {

        Long userId = RequestContext.getUserId();
        String role = RequestContext.getUserRole();

        // 0. 解析 songInfo JSON
        SongUploadDTO songInfo;
        try {
            songInfo = objectMapper.readValue(songInfoJson, SongUploadDTO.class);
        } catch (Exception e) {
            log.error("解析 songInfo JSON 失败: {}", songInfoJson, e);
            throw new BusinessException(ErrorCode.BAD_REQUEST, "歌曲信息格式错误");
        }

        // 1. 校验会员身份
        if (!"VIP".equals(role) && !"ADMIN".equals(role)) {
            throw new BusinessException(ErrorCode.MEMBERSHIP_REQUIRED);
        }

        // 2. 校验歌曲名
        if (StrUtil.isBlank(songInfo.getTitle())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "歌曲名称不能为空");
        }

        // 3. 检查每日上传次数限制（仅检查不递增，Redis 失效时放行）
        int dailyLimit = getDailyUploadLimit();
        checkDailyUploadLimit(userId, dailyLimit);

        // 4. 检查会员存储空间（仅 VIP 需要检查，ADMIN 不受限；Redis 失效时放行）
        if ("VIP".equals(role)) {
            long totalFileSize = audioFile.getSize()
                    + (coverFile != null ? coverFile.getSize() : 0)
                    + (lyricFile != null ? lyricFile.getSize() : 0);
            checkMemberStorage(userId, totalFileSize);
        }

        // 5. 上传文件 → 获取 URL（磁盘 I/O，不参与事务）
        String audioUrl = fileService.upload(audioFile, "audio", null);
        log.info("音频上传完成: {}", audioUrl);

        String coverUrl = null;
        if (coverFile != null && !coverFile.isEmpty()) {
            coverUrl = fileService.upload(coverFile, "cover", null);
            log.info("封面上传完成: {}", coverUrl);
        }

        String lyricUrl = null;
        if (lyricFile != null && !lyricFile.isEmpty()) {
            lyricUrl = fileService.upload(lyricFile, "lyric", null);
            log.info("歌词上传完成: {}", lyricUrl);
        }

        // 6. 从已保存的完整文件中提取音频时长
        //    音频文件已通过 fileService.upload 写入磁盘，Tika 读取完整文件可准确获取时长
        int duration = 0;
        try {
            String audioFilePath = uploadDir + audioUrl.substring("/api/files".length());
            duration = AudioUtils.extractDurationFromFile(audioFilePath);
        } catch (Exception e) {
            log.warn("从已保存文件提取时长失败，默认 0: userId={}, fileName={}", userId, audioFile.getOriginalFilename(), e);
        }
        log.info("音频时长: {} 秒, file={}", duration, audioFile.getOriginalFilename());

        // 7. 组装艺人/专辑/歌曲实体，设置音频时长
        Artist artist = buildArtist(songInfo);
        Album album = buildAlbum(songInfo, coverUrl);

        Song song = new Song();
        song.setTitle(songInfo.getTitle());
        song.setDuration(duration);
        song.setAudioUrl(audioUrl);
        song.setCoverUrl(coverUrl);
        song.setLyricUrl(lyricUrl);
        song.setGenre(songInfo.getGenre());
        song.setLanguage(songInfo.getLanguage());
        song.setReleaseYear(songInfo.getReleaseYear());
        song.setUploaderId(userId);
        // 管理员上传直接 ACTIVE，VIP 用户上传需审核
        song.setStatus("ADMIN".equals(role) ? "ACTIVE" : "PENDING");

        // 7.1 解析分类 ID（从 genre/language/releaseYear 映射到 category 表）
        song.setCategoryIds(resolveCategoryIds(songInfo));

        // 8. 事务性提交到数据库（艺人/专辑/歌曲原子写入）
        SongVO result = songService.submitSongForReview(song, artist, album);
        log.info("歌曲提审成功: id={}, title={}", result.getId(), result.getTitle());

        // 9. 上传成功 → 递增上传计数（放在所有步骤之后，避免失败却消耗次数）
        long todayCount = incrementDailyUploadCount(userId);
        int remaining = Math.max(0, dailyLimit - (int) todayCount);

        // 10. 更新会员存储使用量（Redis，异步计数，不影响 DB 事务）
        if ("VIP".equals(role)) {
            long totalFileSize = audioFile.getSize()
                    + (coverFile != null ? coverFile.getSize() : 0)
                    + (lyricFile != null ? lyricFile.getSize() : 0);
            updateMemberStorage(userId, totalFileSize);
        }

        // 11. 组装响应
        Map<String, Object> data = new HashMap<>();
        data.put("song", result);
        data.put("remainingUploads", remaining);
        data.put("dailyLimit", dailyLimit);

        return R.ok("上传成功" + ("ADMIN".equals(role) ? "" : "，等待管理员审核")
                + "（今日还可上传 " + remaining + " 次）", data);
    }

    /**
     * 我的音乐库（查看自己上传的音乐）
     */
    @GetMapping("/music")
    public R<List<SongVO>> myMusic() {
        Long userId = RequestContext.getUserId();
        List<SongVO> songs = songService.getSongsByUploader(userId);
        return R.ok(songs);
    }

    // ==================== 上传次数限制 ====================

    /**
     * 获取每日上传次数限制（从 system_config 读取，Redis 缓存）
     * <p>
     * 读取 {@code member_upload_limit} 配置项，缓存 60 秒。
     * Redis 或 DB 不可用时回退 DEFAULT_UPLOAD_LIMIT（5）。
     *
     * @return 每日上传次数限制
     */
    private int getDailyUploadLimit() {
        try {
            // 先尝试从 Redis 缓存读取
            String cacheKey = RedisKeys.SYSTEM_CONFIG + CONFIG_KEY_UPLOAD_LIMIT;
            String cached = cacheUtil.get(cacheKey, String.class);
            if (cached != null) {
                return Integer.parseInt(cached);
            }

            // 缓存未命中，从数据库读取
            SystemConfig config = systemConfigMapper.selectByKey(CONFIG_KEY_UPLOAD_LIMIT);
            if (config != null && StrUtil.isNotBlank(config.getConfigValue())) {
                // 写入 Redis 缓存
                cacheUtil.set(cacheKey, config.getConfigValue(), CONFIG_CACHE_TTL, TimeUnit.SECONDS);
                return Integer.parseInt(config.getConfigValue());
            }
        } catch (Exception e) {
            log.warn("读取上传次数配置失败，回退默认值 {}", DEFAULT_UPLOAD_LIMIT, e);
        }
        return DEFAULT_UPLOAD_LIMIT;
    }

    /**
     * 检查每日上传次数限制（仅读取当前计数，不递增）
     * <p>
     * 依赖 Redis 计数，若 Redis 不可用则放行（仅记录警告）避免接口返回 500。
     * 递增操作延迟到上传成功后在 {@link #incrementDailyUploadCount(Long)} 中执行，
     * 避免上传失败却消耗上传次数。
     *
     * @param userId     用户 ID
     * @param dailyLimit 每日限制次数
     * @throws BusinessException 如果已达上限
     */
    private void checkDailyUploadLimit(Long userId, int dailyLimit) {
        try {
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String uploadCountKey = RedisKeys.UPLOAD_COUNT + userId + ":" + today;
            String val = cacheUtil.get(uploadCountKey, String.class);
            long todayCount = val != null ? Long.parseLong(val) : 0L;
            if (todayCount >= dailyLimit) {
                throw new BusinessException(ErrorCode.UPLOAD_LIMIT_EXCEEDED);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Redis 不可用，跳过上传次数限制检查: userId={}", userId, e);
        }
    }

    /**
     * 上传成功后递增每日上传计数（原子 INCR）
     * <p>
     * 仅在文件上传和 DB 写入全部成功后才调用，
     * 确保上传失败不会浪费每日次数。
     *
     * @param userId 用户 ID
     * @return 递增后的总次数
     */
    private long incrementDailyUploadCount(Long userId) {
        try {
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String uploadCountKey = RedisKeys.UPLOAD_COUNT + userId + ":" + today;
            long count = cacheUtil.increment(uploadCountKey, 1);
            if (count == 1) {
                cacheUtil.expire(uploadCountKey, 86400, TimeUnit.SECONDS);
            }
            return count;
        } catch (Exception e) {
            log.warn("Redis 不可用，跳过上传计数更新: userId={}", userId, e);
            return 0;
        }
    }

    // ==================== 存储空间 ====================

    /**
     * 检查会员存储空间是否充足
     * <p>
     * 验证新增文件后总使用量不超过 MEMBER_STORAGE_LIMIT（500MB）。
     * 依赖 Redis，若 Redis 不可用则放行（仅记录警告）避免接口返回 500。
     *
     * @param userId         用户 ID
     * @param newFileSize    本次新增文件的总字节数
     * @throws BusinessException 如果存储空间不足
     */
    private void checkMemberStorage(Long userId, long newFileSize) {
        try {
            String storageKey = RedisKeys.MEMBER_STORAGE + userId;
            String val = cacheUtil.get(storageKey, String.class);
            long currentUsage = val != null ? Long.parseLong(val) : 0L;

            if (currentUsage + newFileSize > MEMBER_STORAGE_LIMIT) {
                long remainingMB = (MEMBER_STORAGE_LIMIT - currentUsage) / (1024 * 1024);
                log.warn("会员存储空间不足: userId={}, current={}, new={}, remaining={}MB",
                        userId, currentUsage, newFileSize, remainingMB);
                throw new BusinessException(ErrorCode.STORAGE_LIMIT_EXCEEDED,
                        "存储空间不足（剩余约 " + remainingMB + "MB），请清理后重试");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Redis 不可用，跳过存储空间检查: userId={}", userId, e);
        }
    }

    /**
     * 更新会员存储使用量
     * <p>
     * 上传成功后增加存储计数，使用 INCRBY 累加。
     * 若 Redis 不可用仅记录警告，不影响上传成功流程。
     */
    private void updateMemberStorage(Long userId, long fileSize) {
        try {
            String storageKey = RedisKeys.MEMBER_STORAGE + userId;
            cacheUtil.increment(storageKey, fileSize);
            // 设置 30 天 TTL 防止一直累积（定时任务会持久化实际使用量）
            cacheUtil.expire(storageKey, 2592000, TimeUnit.SECONDS);
            log.debug("更新会员存储使用量: userId={}, added={}", userId, fileSize);
        } catch (Exception e) {
            log.warn("Redis 不可用，跳过存储使用量更新: userId={}", userId, e);
        }
    }

    // ==================== 实体构建 ====================

    /**
     * 构建艺人实体（从 DTO 中提取 ID 或名称信息，不执行数据库操作）
     * <p>
     * 将艺人查询/创建的 DB 操作统一交由 {@link SongService#submitSongForReview} 的事务管理，
     * 控制器层只负责参数提取和实体组装，降低耦合。
     */
    private Artist buildArtist(SongUploadDTO dto) {
        if (dto.getArtistId() != null) {
            // 已有艺人 ID：交由事务层校验
            Artist artist = new Artist();
            artist.setId(dto.getArtistId());
            return artist;
        }
        if (StrUtil.isBlank(dto.getArtistName())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "必须指定艺人（ID 或名称）");
        }
        Artist artist = new Artist();
        artist.setName(dto.getArtistName().trim());
        return artist;
    }

    /**
     * 解析分类 ID 列表（从 genre/language/releaseYear 映射到 category 表）
     * <p>
     * 将前端传入的风格、语种、发行年份文本，转换为 category 表中对应的 ID，
     * 后续由 {@link SongService#submitSongForReview} 在事务中写入 song_category 关联表。
     *
     * @param dto 歌曲上传 DTO
     * @return 分类 ID 列表（可能为空）
     */
    private List<Long> resolveCategoryIds(SongUploadDTO dto) {
        List<Long> ids = new ArrayList<>();
        try {
            // 风格 → GENRE 分类
            if (StrUtil.isNotBlank(dto.getGenre())) {
                Category cat = categoryMapper.selectByNameAndType(dto.getGenre().trim(), "GENRE");
                if (cat != null) ids.add(cat.getId());
            }
            // 语种 → LANGUAGE 分类
            if (StrUtil.isNotBlank(dto.getLanguage())) {
                Category cat = categoryMapper.selectByNameAndType(dto.getLanguage().trim(), "LANGUAGE");
                if (cat != null) ids.add(cat.getId());
            }
            // 发行年份 → YEAR 分类（如 2020 → "20年代"）
            if (dto.getReleaseYear() != null) {
                int decade = (dto.getReleaseYear() / 10) * 10;
                String yearCategory = decade + "年代";
                Category cat = categoryMapper.selectByNameAndType(yearCategory, "YEAR");
                if (cat != null) ids.add(cat.getId());
            }
        } catch (Exception e) {
            log.warn("解析分类 ID 失败，跳过分类关联: {}", e.getMessage());
        }
        return ids;
    }

    /**
     * 构建专辑实体（从 DTO 中提取 ID 或标题信息，不执行数据库操作）
     * <p>
     * 专辑查询/创建的 DB 操作统一交由 {@link SongService#submitSongForReview} 的事务管理。
     */
    private Album buildAlbum(SongUploadDTO dto, String coverUrl) {
        if (dto.getAlbumId() != null) {
            // 已有专辑 ID：交由事务层校验
            Album album = new Album();
            album.setId(dto.getAlbumId());
            return album;
        }
        if (StrUtil.isBlank(dto.getAlbumTitle())) {
            return null; // 无专辑信息
        }
        Album album = new Album();
        album.setTitle(dto.getAlbumTitle().trim());
        album.setCoverUrl(coverUrl);
        if (dto.getReleaseYear() != null) {
            album.setReleaseDate(java.time.LocalDate.of(dto.getReleaseYear(), 1, 1));
        }
        return album;
    }
}
