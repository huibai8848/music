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
 * 通知 WebSocket 握手拦截器
 * <p>
 * 在 WebSocket 连接建立前对 token 进行鉴权。
 * 解析 URL 中的 token 参数，验证 JWT 有效性，提取 userId 存入 session 属性。
 * <p>
 * 连接地址：ws://host/ws/notify?token={jwt}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyWebSocketInterceptor implements HandshakeInterceptor {

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

            String path = httpRequest.getRequestURI();
            log.debug("【通知WS】握手请求: path={}", path);
        }

        if (token == null || token.isEmpty()) {
            log.warn("【通知WS】握手失败: token 为空");
            return false;
        }

        // 验证 JWT
        try {
            // 1. 校验 token 有效性
            if (!jwtUtil.validateToken(token)) {
                log.warn("【通知WS】握手失败: token 无效或已过期");
                return false;
            }

            // 2. 检查 Token 是否在黑名单中（已登出）
            String jti = jwtUtil.getJtiFromToken(token);
            if (jti != null && cacheUtil.exists(RedisKeys.BL_TOKEN + jti)) {
                log.warn("【通知WS】握手失败: token 已登出");
                return false;
            }

            Long userId = jwtUtil.getUserIdFromToken(token);
            String role = jwtUtil.getRoleFromToken(token);

            if (userId == null) {
                log.warn("【通知WS】握手失败: 无法提取用户 ID");
                return false;
            }

            // 将 userId 和 role 存入 session 属性，供后续 Handler 使用
            attributes.put("userId", userId);
            attributes.put("role", role);
            log.debug("【通知WS】握手成功: userId={}, role={}", userId, role);
            return true;

        } catch (Exception e) {
            log.warn("【通知WS】握手失败: token 校验异常", e);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 握手完成后无需额外处理
    }
}
