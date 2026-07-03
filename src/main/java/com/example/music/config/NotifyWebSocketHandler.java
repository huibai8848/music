package com.example.music.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * 通知 WebSocket 消息处理器
 * <p>
 * 处理通知推送的 WebSocket 连接生命周期：
 * 1. afterConnectionEstablished — 连接建立后注册会话到 {@link NotifySessionManager}
 * 2. handleTextMessage — 处理客户端心跳（ping/pong）
 * 3. afterConnectionClosed — 连接关闭后清理会话
 * <p>
 * 此通道为单向推送（仅服务端 → 客户端），
 * 客户端无需发送业务消息，只需定期发送心跳维持连接。
 * <p>
 * 端点：/ws/notify?token={jwt}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyWebSocketHandler extends TextWebSocketHandler {

    private final NotifySessionManager notifySessionManager;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = getUserIdFromSession(session);
        if (userId != null) {
            notifySessionManager.register(userId, session);
            log.info("【通知WS】用户 {} 已连接，当前在线: {}", userId, notifySessionManager.countOnline());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 通知通道是单向的（仅服务端推送），客户端发来的消息只做心跳处理
        String payload = message.getPayload();
        if ("ping".equalsIgnoreCase(payload) || "HEARTBEAT".equals(payload)) {
            try {
                session.sendMessage(new TextMessage("pong"));
            } catch (Exception e) {
                log.warn("【通知WS】Pong 发送失败");
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = getUserIdFromSession(session);
        if (userId != null) {
            notifySessionManager.remove(userId, session);
            log.info("【通知WS】用户 {} 断开连接，当前在线: {}", userId, notifySessionManager.countOnline());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        Long userId = getUserIdFromSession(session);
        log.warn("【通知WS】传输异常 userId={}", userId, exception);
        if (userId != null) {
            notifySessionManager.remove(userId, session);
        }
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
}