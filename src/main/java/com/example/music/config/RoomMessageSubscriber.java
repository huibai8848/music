package com.example.music.config;

import com.example.music.service.RoomService;
import com.example.music.vo.WebSocketMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 歌房消息 Redis 订阅者
 * <p>
 * 监听 Redis Pub/Sub 频道中的歌房相关消息，
 * 通过 RoomSessionManager 将消息广播到房间内所有成员的 WebSocket 连接。
 * <p>
 * 工作流程：
 * <pre>
 *   WebSocket Handler 发布 → Redis Channel → 所有服务实例收到
 *     → RoomMessageSubscriber → RoomSessionManager.broadcastToRoom()
 * </pre>
 * <p>
 * 当部署多个后端实例时，此机制确保跨实例的成员都能收到消息。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoomMessageSubscriber {

    private final RoomSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    /**
     * 处理歌房同步消息（播放/暂停/切歌/进度）
     * <p>
     * 由 RedisPubSubConfig 中的路由逻辑调用。
     *
     * @param message 消息 JSON
     */
    public void handleRoomSync(String message) {
        try {
            WebSocketMessage wsMsg = objectMapper.readValue(message, WebSocketMessage.class);
            Long senderId = wsMsg.getUserId();

            // 从 payload 中提取 roomId
            Long roomId = extractRoomId(wsMsg);
            if (roomId == null) return;

            // 查找发送者的 WebSocket 会话 ID
            String senderSessionId = findSenderSessionId(roomId, senderId);

            // 广播给房间内其他成员（排除发送者，避免回显）
            sessionManager.broadcastToRoom(roomId, message, senderSessionId);

            log.debug("【房间同步】广播到房间 roomId={}, senderId={}", roomId, senderId);
        } catch (Exception e) {
            log.error("处理房间同步消息异常: {}", message, e);
        }
    }

    /**
     * 处理聊天消息
     */
    public void handleRoomChat(String message) {
        try {
            WebSocketMessage wsMsg = objectMapper.readValue(message, WebSocketMessage.class);
            Long senderId = wsMsg.getUserId();
            Long roomId = extractRoomId(wsMsg);
            if (roomId == null) return;

            String senderSessionId = findSenderSessionId(roomId, senderId);
            sessionManager.broadcastToRoom(roomId, message, senderSessionId);

            log.debug("【房间聊天】广播到房间 roomId={}", roomId);
        } catch (Exception e) {
            log.error("处理房间聊天消息异常: {}", message, e);
        }
    }

    /**
     * 处理成员变更消息（加入/离开/踢出）
     */
    public void handleRoomMember(String message) {
        try {
            WebSocketMessage wsMsg = objectMapper.readValue(message, WebSocketMessage.class);
            Long roomId = extractRoomId(wsMsg);
            if (roomId == null) return;

            // 成员变更广播给所有成员（包括变更者）
            sessionManager.broadcastToRoom(roomId, message);

            log.debug("【房间成员】广播到房间 roomId={}", roomId);
        } catch (Exception e) {
            log.error("处理房间成员消息异常: {}", message, e);
        }
    }

    /**
     * 从消息 payload 中提取 roomId
     */
    private Long extractRoomId(WebSocketMessage wsMsg) {
        if (wsMsg.getPayload() instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> payload =
                    (java.util.Map<String, Object>) wsMsg.getPayload();
            Object roomIdObj = payload.get("roomId");
            if (roomIdObj != null) {
                if (roomIdObj instanceof Number) {
                    return ((Number) roomIdObj).longValue();
                }
                try {
                    return Long.parseLong(roomIdObj.toString());
                } catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }

    /**
     * 在房间中查找发送者的 WebSocket 会话 ID
     */
    private String findSenderSessionId(Long roomId, Long senderId) {
        if (senderId == null) return null;

        // 遍历房间内会话，找到发送者的第一个会话
        var sessions = sessionManager.getRoomSessions(roomId);
        for (var session : sessions) {
            if (senderId.equals(sessionManager.getUserId(session.getId()))) {
                return session.getId();
            }
        }
        return null;
    }
}
