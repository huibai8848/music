package com.example.music.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

/**
 * WebSocket 配置
 * <p>
 * 注册 WebSocket 端点：
 * <ul>
 *   <li>/ws/room/{roomId} — 歌房 WebSocket（聊天、播放同步）</li>
 *   <li>/ws/notify — 通知推送 WebSocket（单向服务端推送）</li>
 * </ul>
 * 连接地址：ws://host/ws/{endpoint}?token=xxx
 * <p>
 * setAllowedOrigins("*") 允许跨域连接（前端开发环境使用）。
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final RoomWebSocketHandler roomWebSocketHandler;
    private final RoomWebSocketInterceptor roomWebSocketInterceptor;
    private final NotifyWebSocketHandler notifyWebSocketHandler;
    private final NotifyWebSocketInterceptor notifyWebSocketInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 歌房 WebSocket
        registry.addHandler(roomWebSocketHandler, "/ws/room/{roomId}")
                .addInterceptors(roomWebSocketInterceptor)
                .setAllowedOrigins("*");

        // 通知推送 WebSocket
        registry.addHandler(notifyWebSocketHandler, "/ws/notify")
                .addInterceptors(notifyWebSocketInterceptor)
                .setAllowedOrigins("*");
    }

    /**
     * 配置 WebSocket 容器参数（帧大小、消息大小、会话超时）
     * <p>
     * maxTextMessageBufferSize / maxBinaryMessageBufferSize: 限制单条消息大小，防止 OOM
     * maxSessionIdleTimeout: 会话空闲超时（毫秒），超过无心跳则自动关闭
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(65536);   // 64KB
        container.setMaxBinaryMessageBufferSize(65536);  // 64KB
        container.setMaxSessionIdleTimeout(600000L);     // 10 分钟
        return container;
    }
}
