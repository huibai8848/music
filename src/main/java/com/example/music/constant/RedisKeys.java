package com.example.music.constant;

/**
 * Redis Key 常量定义
 * <p>
 * 所有 Redis Key 统一定义在此，确保命名一致、方便维护。
 * 命名格式：{业务域}:{具体标识}[:{子标识}]
 */
public interface RedisKeys {

    // ==================== Token 认证 ====================

    /** Token 黑名单（key = bl_token:{jti}，TTL = token 剩余有效期） */
    String BL_TOKEN = "bl_token:";

    /** Refresh Token（key = refresh_token:{userId}，TTL = 7 天） */
    String REFRESH_TOKEN = "refresh_token:";

    // ==================== 缓存 ====================

    /** 热门歌曲列表（TTL = 300s） */
    String CACHE_HOT_SONGS = "cache:hot_songs";

    /** 歌词缓存（key = cache:lyrics:{songId}，TTL = 1800s） */
    String CACHE_LYRICS = "cache:lyrics:";

    /** 歌单详情缓存（key = cache:playlist:{id}，TTL = 180s） */
    String CACHE_PLAYLIST = "cache:playlist:";

    /** 首页推荐（TTL = 600s） */
    String CACHE_HOME_RECOMMEND = "cache:home_recommend";

    /** 搜索结果（key = cache:search:{queryMD5}，TTL = 60s） */
    String CACHE_SEARCH = "cache:search:";

    // ==================== 歌房 ====================

    /** 歌房状态（Hash，room:{roomId}） */
    String ROOM = "room:";

    /** 歌房成员（Set，room:{roomId}:members） */
    String ROOM_MEMBERS = "room:%s:members";

    /** 歌房播放队列（String/JSON，room:{roomId}:queue） */
    String ROOM_QUEUE = "room:%s:queue";

    // ==================== 限流 ====================

    /** API 限流（String，key = ratelimit:{userId}:{apiPath}，TTL = 60s） */
    String RATE_LIMIT = "ratelimit:";

    // ==================== 在线状态 ====================

    /** 用户在线状态（String，online:{userId}，TTL = 300s，心跳续期） */
    String ONLINE_USER = "online:";

    // ==================== 计数 ====================

    /** 每日上传计数（key = upload_count:{userId}:{date}） */
    String UPLOAD_COUNT = "upload_count:";

    /** 系统配置缓存（key = sys_config:{configKey}，TTL = 60s） */
    String SYSTEM_CONFIG = "sys_config:";

    // ==================== 排行榜 ====================

    /** 日榜 ZSet（ranking:daily:{yyyyMMdd}） */
    String RANKING_DAILY = "ranking:daily:";
    /** 周榜 ZSet（ranking:weekly:{yyyy'w'ww}） */
    String RANKING_WEEKLY = "ranking:weekly:";
    /** 月榜 ZSet（ranking:monthly:{yyyyMM}） */
    String RANKING_MONTHLY = "ranking:monthly:";
    /** 总榜 ZSet */
    String RANKING_ALL = "ranking:alltime";

    // ==================== 播放进度 ====================

    /** 播放进度（String，progress:{userId}:{songId}，TTL = 7 天） */
    String PLAY_PROGRESS = "progress:";

    // ==================== TTL 常量（秒） ====================

    /** 热门缓存 TTL */
    long TTL_HOT_SONGS = 300;
    /** 歌词缓存 TTL */
    long TTL_LYRICS = 1800;
    /** 歌单缓存 TTL */
    long TTL_PLAYLIST = 180;
    /** 首页推荐 TTL */
    long TTL_HOME = 600;
    /** 搜索缓存 TTL */
    long TTL_SEARCH = 60;
    /** 播放进度 TTL（7 天） */
    long TTL_PROGRESS = 604800;
    /** 在线状态 TTL */
    long TTL_ONLINE = 300;
    /** 限流窗口 TTL */
    long TTL_RATE_LIMIT = 60;

    // ==================== 会员存储 ====================

    /** 会员存储使用量（storage:{userId}，用于 VIP 存储限制 500MB 追踪） */
    String MEMBER_STORAGE = "storage:";
}