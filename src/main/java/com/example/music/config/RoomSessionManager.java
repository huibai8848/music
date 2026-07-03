package com.example.music.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket 会话管理器
 * <p>
 * 管理所有歌房的 WebSocket 连接会话。
 * 维护三个层面的映射关系：
 * 1. sessionId → WebSocketSession（全局会话池）
 * 2. roomId → Set&lt;sessionId&gt;（房间会话索引，用于广播）
 * 3. userId → Set&lt;sessionId&gt;（用户会话索引，用于查找用户）
 * <p>
 * 线程安全，支持并发读写。
 */
@Slf4j
@Component
public class RoomSessionManager {

    /** 全局会话池：sessionId → WebSocketSession */
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /** 房间会话索引：roomId → Set&lt;sessionId&gt; */
    private final Map<Long, Set<String>> roomSessions = new ConcurrentHashMap<>();

    /** 用户会话索引：userId → Set&lt;sessionId&gt; */
    private final Map<Long, Set<String>> userSessions = new ConcurrentHashMap<>();

    /** 会话所属房间：sessionId → roomId */
    private final Map<String, Long> sessionRoomMap = new ConcurrentHashMap<>();

    /** 会话所属用户：sessionId → userId */
    private final Map<String, Long> sessionUserMap = new ConcurrentHashMap<>();

    /**
     * 注册会话
     *
     * @param roomId  房间 ID
     * @param userId  用户 ID
     * @param session WebSocket 会话
     */
    public void register(Long roomId, Long userId, WebSocketSession session) {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        sessionRoomMap.put(sessionId, roomId);
        sessionUserMap.put(sessionId, userId);

        roomSessions.computeIfAbsent(roomId, k -> new CopyOnWriteArraySet<>()).add(sessionId);
        userSessions.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(sessionId);

        log.debug("WebSocket 会话注册: sessionId={}, roomId={}, userId={}", sessionId, roomId, userId);
    }

    /**
     * 移除会话
     *
     * @param sessionId 会话 ID
     */
    public void remove(String sessionId) {
        WebSocketSession session = sessions.remove(sessionId);

        Long roomId = sessionRoomMap.remove(sessionId);
        Long userId = sessionUserMap.remove(sessionId);

        if (roomId != null) {
            Set<String> roomSet = roomSessions.get(roomId);
            if (roomSet != null) {
                roomSet.remove(sessionId);
                if (roomSet.isEmpty()) {
                    roomSessions.remove(roomId);
                }
            }
        }

        if (userId != null) {
            Set<String> userSet = userSessions.get(userId);
            if (userSet != null) {
                userSet.remove(sessionId);
                if (userSet.isEmpty()) {
                    userSessions.remove(userId);
                }
            }
        }

        log.debug("WebSocket 会话移除: sessionId={}, roomId={}, userId={}", sessionId, roomId, userId);
    }

    /**
     * 获取会话所属的房间 ID
     */
    public Long getRoomId(String sessionId) {
        return sessionRoomMap.get(sessionId);
    }

    /**
     * 获取会话所属的用户 ID
     */
    public Long getUserId(String sessionId) {
        return sessionUserMap.get(sessionId);
    }

    /**
     * 获取房间的所有会话
     *
     * @param roomId 房间 ID
     * @return 会话集合（可能为空）
     */
    public Set<WebSocketSession> getRoomSessions(Long roomId) {
        Set<String> sessionIds = roomSessions.get(roomId);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return Set.of();
        }
        Set<WebSocketSession> result = new CopyOnWriteArraySet<>();
        for (String sid : sessionIds) {
            WebSocketSession s = sessions.get(sid);
            if (s != null && s.isOpen()) {
                result.add(s);
            }
        }
        return result;
    }

    /**
     * 获取用户的所有会话（支持多端登录）
     */
    public Set<WebSocketSession> getUserSessions(Long userId) {
        Set<String> sessionIds = userSessions.get(userId);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return Set.of();
        }
        Set<WebSocketSession> result = new CopyOnWriteArraySet<>();
        for (String sid : sessionIds) {
            WebSocketSession s = sessions.get(sid);
            if (s != null && s.isOpen()) {
                result.add(s);
            }
        }
        return result;
    }

    /**
     * 向房间内所有成员广播消息（排除发送者）
     *
     * @param roomId      房间 ID
     * @param message     消息内容
     * @param excludeSessionId 排除的会话 ID（发送者不接收自己的广播）
     */
    public void broadcastToRoom(Long roomId, String message, String excludeSessionId) {
        Set<WebSocketSession> roomSessions = getRoomSessions(roomId);
        for (WebSocketSession session : roomSessions) {
            if (session.getId().equals(excludeSessionId)) {
                continue;
            }
            sendMessage(session, message);
        }
    }

    /**
     * 向房间内所有成员广播消息
     */
    public void broadcastToRoom(Long roomId, String message) {
        broadcastToRoom(roomId, message, null);
    }

    /**
     * 向指定用户的所有会话发送消息
     */
    public void sendToUser(Long userId, String message) {
        Set<WebSocketSession> userSessions = getUserSessions(userId);
        for (WebSocketSession session : userSessions) {
            sendMessage(session, message);
        }
    }

    /**
     * 发送消息到单个会话
     */
    public void sendMessage(WebSocketSession session, String message) {
        if (session == null) return;
        synchronized (session) {
            if (!session.isOpen()) return;
            try {
                session.sendMessage(new org.springframework.web.socket.TextMessage(message));
            } catch (IOException e) {
                log.error("发送 WebSocket 消息失败: sessionId={}", session.getId(), e);
            }
        }
    }

    /**
     * 获取当前在线会话总数
     */
    public int getOnlineCount() {
        return sessions.size();
    }

    /**
     * 获取房间在线人数
     */
    public int getRoomOnlineCount(Long roomId) {
        Set<String> sessionIds = roomSessions.get(roomId);
        if (sessionIds == null) return 0;
        return (int) sessionIds.stream()
                .filter(sid -> {
                    WebSocketSession s = sessions.get(sid);
                    return s != null && s.isOpen();
                })
                .count();
    }
}
