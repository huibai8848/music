package com.example.music.constant;

/**
 * Redis Pub/Sub 频道常量定义
 * <p>
 * 所有消息频道统一定义，确保发布者和订阅者使用相同的频道名。
 * 命名格式：{业务域}:{事件类型}
 */
public interface RedisChannels {

    // ==================== 歌房同步 ====================

    /** 歌房播放控制（播放/暂停/切歌/进度拖动） */
    String ROOM_SYNC = "channel:room:sync";

    /** 歌房聊天消息 */
    String ROOM_CHAT = "channel:room:chat";

    /** 歌房成员变更（加入/离开/踢出） */
    String ROOM_MEMBER = "channel:room:member";

    // ==================== 通知推送 ====================

    /** 用户通知（评论回复、系统公告等） */
    String USER_NOTIFY = "channel:notify:user";

    /** 系统广播（全站公告） */
    String SYSTEM_BROADCAST = "channel:notify:broadcast";

    // ==================== 异步任务 ====================

    /** 播放计数落库触发 */
    String PLAY_COUNT_FLUSH = "channel:task:play_count_flush";

    /** 批量任务通知 */
    String BATCH_TASK = "channel:task:batch";
}
