package com.example.music.config;

import com.example.music.constant.RedisChannels;
import com.example.music.service.RoomService;
import com.example.music.utils.JwtUtil;
import com.example.music.vo.WebSocketMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.Map;

/**
 * 歌房 WebSocket 消息处理器
 * <p>
 * 处理 WebSocket 连接生命周期和消息收发：
 * 1. afterConnectionEstablished — 连接建立后注册会话
 * 2. handleTextMessage — 处理收到的消息（CHAT/SYNC/HEARTBEAT）
 * 3. afterConnectionClosed — 连接关闭后清理会话
 * <p>
 * 消息协议见 {@link WebSocketMessage}。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoomWebSocketHandler extends TextWebSocketHandler {

    private final RoomSessionManager sessionManager;
    private final RoomService roomService;
    private final RedisMessagePublisher redisPublisher;
    private final ObjectMapper objectMapper;
    private final JwtUtil jwtUtil;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // 从路径中提取 roomId: ws/room/{roomId}
        Long roomId = extractRoomId(session);
        Long userId = getUserIdFromSession(session);

        if (roomId == null || userId == null) {
            closeSession(session, CloseStatus.BAD_DATA);
            return;
        }

        // 注册会话
        sessionManager.register(roomId, userId, session);

        log.info("WebSocket 连接建立: sessionId={}, roomId={}, userId={}",
                session.getId(), roomId, userId);

        // 通过 Redis 发布成员变更消息
        try {
            WebSocketMessage msg = WebSocketMessage.builder()
                    .type("SYSTEM")
                    .userId(userId)
                    .payload(Map.of("action", "JOIN", "roomId", roomId))
                    .timestamp(System.currentTimeMillis())
                    .build();
            redisPublisher.publish(RedisChannels.ROOM_MEMBER,
                    objectMapper.writeValueAsString(msg));
        } catch (Exception e) {
            log.error("发布成员变更消息失败", e);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String payload = message.getPayload();
            WebSocketMessage wsMsg = objectMapper.readValue(payload, WebSocketMessage.class);

            Long userId = getUserIdFromSession(session);
            Long roomId = sessionManager.getRoomId(session.getId());

            if (userId == null || roomId == null) {
                closeSession(session, CloseStatus.BAD_DATA);
                return;
            }

            // 补充消息头
            wsMsg.setUserId(userId);
            wsMsg.setTimestamp(System.currentTimeMillis());

            String type = wsMsg.getType();

            switch (type) {
                case "HEARTBEAT":
                    handleHeartbeat(session);
                    break;

                case "CHAT":
                    handleChat(roomId, userId, wsMsg);
                    break;

                case "SYNC":
                    handleSync(roomId, userId, wsMsg);
                    break;

                case "VOICE":
                    handleVoice(roomId, userId, wsMsg);
                    break;

                default:
                    log.warn("未知消息类型: type={}", type);
                    break;
            }

        } catch (Exception e) {
            log.error("处理 WebSocket 消息异常", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long roomId = sessionManager.getRoomId(session.getId());
        Long userId = sessionManager.getUserId(session.getId());

        sessionManager.remove(session.getId());

        log.info("WebSocket 连接关闭: sessionId={}, roomId={}, userId={}, status={}",
                session.getId(), roomId, userId, status);

        // 通过 Redis 发布成员变更消息
        if (roomId != null && userId != null) {
            try {
                WebSocketMessage msg = WebSocketMessage.builder()
                        .type("SYSTEM")
                        .userId(userId)
                        .payload(Map.of("action", "LEAVE", "roomId", roomId))
                        .timestamp(System.currentTimeMillis())
                        .build();
                redisPublisher.publish(RedisChannels.ROOM_MEMBER,
                        objectMapper.writeValueAsString(msg));
            } catch (Exception e) {
                log.error("发布成员变更消息失败", e);
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket 传输异常: sessionId={}", session.getId(), exception);
        sessionManager.remove(session.getId());
    }

    // ==================== 消息处理 ====================

    /**
     * 处理心跳：回应 pong
     */
    private void handleHeartbeat(WebSocketSession session) {
        try {
            WebSocketMessage pong = WebSocketMessage.builder()
                    .type("HEARTBEAT")
                    .timestamp(System.currentTimeMillis())
                    .build();
            String json = objectMapper.writeValueAsString(pong);
            sessionManager.sendMessage(session, json);
        } catch (Exception e) {
            log.error("发送心跳响应失败", e);
        }
    }

    /**
     * 处理聊天消息：保存到数据库 + 广播给房间内其他成员
     */
    private void handleChat(Long roomId, Long userId, WebSocketMessage wsMsg) {
        // 提取聊天内容
        String content = "";
        if (wsMsg.getPayload() instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) wsMsg.getPayload();
            Object contentObj = payload.get("content");
            content = contentObj != null ? contentObj.toString() : "";
        }

        // 保存到数据库
        roomService.saveMessage(roomId, userId, "TEXT", content);

        // 通过 Redis 发布聊天消息
        try {
            String json = objectMapper.writeValueAsString(wsMsg);
            redisPublisher.publish(RedisChannels.ROOM_CHAT, json);
        } catch (Exception e) {
            log.error("发布聊天消息失败", e);
        }
    }

    /**
     * 处理播放同步：更新 Redis 状态 + 广播给房间内其他成员
     */
    private void handleSync(Long roomId, Long userId, WebSocketMessage wsMsg) {
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) wsMsg.getPayload();

        Boolean isPlaying = payload.containsKey("isPlaying")
                ? Boolean.parseBoolean(payload.get("isPlaying").toString()) : null;
        Long songId = payload.containsKey("songId")
                ? Long.parseLong(payload.get("songId").toString()) : null;
        Integer progress = payload.containsKey("progress")
                ? Integer.parseInt(payload.get("progress").toString()) : null;

        // 更新 Redis 状态
        roomService.updatePlayback(roomId, userId, isPlaying, songId, progress);

        // 通过 Redis 发布同步消息
        try {
            String json = objectMapper.writeValueAsString(wsMsg);
            redisPublisher.publish(RedisChannels.ROOM_SYNC, json);
        } catch (Exception e) {
            log.error("发布同步消息失败", e);
        }
    }

    /**
     * 处理语音消息：广播给房间内其他成员
     */
    private void handleVoice(Long roomId, Long userId, WebSocketMessage wsMsg) {
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) wsMsg.getPayload();
        String voiceUrl = payload != null ? (String) payload.get("url") : "";

        if (voiceUrl != null && !voiceUrl.isEmpty()) {
            roomService.saveMessage(roomId, userId, "VOICE", voiceUrl);
        }

        try {
            String json = objectMapper.writeValueAsString(wsMsg);
            redisPublisher.publish(RedisChannels.ROOM_CHAT, json);
        } catch (Exception e) {
            log.error("发布语音消息失败", e);
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 从 session 路径中提取 roomId: ws/room/{roomId}
     */
    private Long extractRoomId(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) return null;
        String path = uri.getPath();
        // /ws/room/{roomId}
        String[] parts = path.split("/");
        if (parts.length >= 4) {
            try {
                return Long.parseLong(parts[3]);
            } catch (NumberFormatException e) {
                log.warn("提取 roomId 失败: path={}", path);
            }
        }
        return null;
    }

    /**
     * 从 session 属性中获取 userId
     */
    private Long getUserIdFromSession(WebSocketSession session) {
        Object userIdAttr = session.getAttributes().get("userId");
        if (userIdAttr instanceof Long) {
            return (Long) userIdAttr;
        }
        return null;
    }

    /**
     * 关闭会话
     */
    private void closeSession(WebSocketSession session, CloseStatus status) {
        try {
            session.close(status);
        } catch (Exception e) {
            log.warn("关闭 WebSocket 会话异常", e);
        }
        sessionManager.remove(session.getId());
    }
}
