package com.example.music.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 房间消息视图对象
 * <p>
 * 用于 API 返回房间内历史消息（最近 50 条）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomMessageVO {

    /** 消息 ID */
    private Long id;

    /** 房间 ID */
    private Long roomId;

    /** 发送者 ID */
    private Long userId;

    /** 发送者昵称 */
    private String nickname;

    /** 消息类型：TEXT / VOICE / SYSTEM */
    private String type;

    /** 消息内容 */
    private String content;

    /** 发送时间（时间戳，毫秒） */
    private Long createdAt;

    /**
     * 从实体构建 VO
     *
     * @param message  消息实体
     * @param nickname 发送者昵称
     * @return RoomMessageVO
     */
    public static RoomMessageVO fromEntity(com.example.music.entity.RoomMessage message, String nickname) {
        if (message == null) return null;
        return RoomMessageVO.builder()
                .id(message.getId())
                .roomId(message.getRoomId())
                .userId(message.getUserId())
                .nickname(nickname)
                .type(message.getType())
                .content(message.getContent())
                .createdAt(message.getCreatedTime() != null
                        ? message.getCreatedTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                        : null)
                .build();
    }
}
