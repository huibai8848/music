package com.example.music.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 房间消息实体
 * <p>
 * 对应表 room_message，用于持久化房间内的聊天消息和系统消息。
 * 消息类型包括：TEXT（文字聊天）、VOICE（语音消息）、SYSTEM（系统消息）。
 * 保留最近的消息记录供新加入成员查看历史。
 */
@Data
public class RoomMessage {

    /** 主键 ID */
    private Long id;

    /** 房间 ID */
    private Long roomId;

    /** 发送者 ID */
    private Long userId;

    /** 消息类型：TEXT / VOICE / SYSTEM */
    private String type;

    /** 消息内容（文本内容或语音文件 URL） */
    private String content;

    /** 创建时间 */
    private LocalDateTime createdTime;
}
