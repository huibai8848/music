package com.example.music.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 通知 WebSocket 会话管理器
 * <p>
 * 管理用户通知的 WebSocket 连接。
 * 每个用户可以有多个连接（多设备登录）。
 * <p>
 * 线程安全，支持并发读写。
 */
@Slf4j
@Component
public class NotifySessionManager {

    /** userId -> Set<WebSocketSession> */
    private final ConcurrentHashMap<Long, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    /**
     * 注册用户连接
     */
    public void register(Long userId, WebSocketSession session) {
        userSessions.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(session);
        log.debug("【通知WS】用户 {} 连接，当前在线: {}", userId, countOnline());
    }

    /**
     * 移除用户连接
     */
    public void remove(Long userId, WebSocketSession session) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                userSessions.remove(userId);
            }
        }
        log.debug("【通知WS】用户 {} 断开，当前在线: {}", userId, countOnline());
    }

    /**
     * 根据 sessionId 移除会话
     */
    public void removeBySessionId(String sessionId) {
        userSessions.forEach((userId, sessions) -> {
            sessions.removeIf(s -> s.getId().equals(sessionId));
            if (sessions.isEmpty()) {
                userSessions.remove(userId);
            }
        });
    }

    /**
     * 向用户的所有设备推送通知
     *
     * @param userId  目标用户 ID
     * @param message 消息内容
     * @return true=至少推送到一个设备
     */
    public boolean sendToUser(Long userId, String message) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return false;
        }
        boolean sent = false;
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    synchronized (session) {
                        session.sendMessage(new TextMessage(message));
                    }
                    sent = true;
                } catch (IOException e) {
                    log.warn("【通知WS】推送失败 userId={}, 移除会话", userId);
                    sessions.remove(session);
                }
            } else {
                sessions.remove(session);
            }
        }
        return sent;
    }

    /**
     * 广播给所有在线用户
     *
     * @param message 消息内容
     */
    public void broadcast(String message) {
        userSessions.forEach((userId, sessions) -> {
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        synchronized (session) {
                            session.sendMessage(new TextMessage(message));
                        }
                    } catch (IOException e) {
                        log.warn("【通知WS】广播推送失败 userId={}", userId);
                        sessions.remove(session);
                    }
                } else {
                    sessions.remove(session);
                }
            }
        });
    }

    /**
     * 获取在线用户数
     */
    public int countOnline() {
        return (int) userSessions.values().stream()
                .filter(s -> !s.isEmpty())
                .count();
    }
}