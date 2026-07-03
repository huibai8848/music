package com.example.music.config;

import com.example.music.constant.RedisKeys;
import com.example.music.utils.CacheUtil;
import com.example.music.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手拦截器
 * <p>
 * 在 WebSocket 连接建立前对 token 进行鉴权。
 * 解析 URL 中的 token 参数，验证 JWT 有效性，提取 userId 存入 session 属性。
 * <p>
 * 连接地址：ws://host/ws/room/{roomId}?token=xxx
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoomWebSocketInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;
    private final CacheUtil cacheUtil;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {

        // 从 URL 中获取 token 参数
        String token = null;
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpRequest = servletRequest.getServletRequest();
            token = httpRequest.getParameter("token");

            // 同时也从 roomId 路径中提取
            String path = httpRequest.getRequestURI();
            log.debug("WebSocket 握手请求: path={}", path);
        }

        if (token == null || token.isEmpty()) {
            log.warn("WebSocket 握手失败: token 为空");
            return false;
        }

        // 验证 JWT
        try {
            // 1. 校验 token 有效性
            if (!jwtUtil.validateToken(token)) {
                log.warn("WebSocket 握手失败: token 无效或已过期");
                return false;
            }

            // 2. 检查 Token 是否在黑名单中（已登出）
            String jti = jwtUtil.getJtiFromToken(token);
            if (jti != null && cacheUtil.exists(RedisKeys.BL_TOKEN + jti)) {
                log.warn("WebSocket 握手失败: token 已登出");
                return false;
            }

            Long userId = jwtUtil.getUserIdFromToken(token);
            String role = jwtUtil.getRoleFromToken(token);

            if (userId == null) {
                log.warn("WebSocket 握手失败: 无效 token");
                return false;
            }

            // 将 userId 和 role 存入 session 属性，供后续 Handler 使用
            attributes.put("userId", userId);
            attributes.put("role", role);
            log.debug("WebSocket 握手成功: userId={}, role={}", userId, role);
            return true;

        } catch (Exception e) {
            log.warn("WebSocket 握手失败: token 校验异常", e);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 握手完成后无需额外处理
    }
}
