package com.example.music.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket 消息协议
 * <p>
 * 歌房模块 WebSocket 通信的统一消息格式。
 * 所有通过 WebSocket 传输的消息都使用此结构。
 * <p>
 * type 字段取值：
 * - CHAT      ：文字聊天消息
 * - SYNC      ：播放控制同步（播放/暂停/切歌/进度拖动）
 * - SYSTEM    ：系统消息（成员加入/离开/踢出/房主移交等）
 * - HEARTBEAT ：心跳检测
 * - VOICE     ：语音消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {

    /** 消息类型 */
    private String type;

    /** 发送者 ID */
    private Long userId;

    /** 发送者昵称 */
    private String nickname;

    /** 消息负载（不同 type 有不同结构） */
    private Object payload;

    /** 时间戳（毫秒） */
    private Long timestamp;
}
