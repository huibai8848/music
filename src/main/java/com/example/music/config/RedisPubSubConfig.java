package com.example.music.config;

import com.example.music.constant.RedisChannels;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

/**
 * Redis Pub/Sub 配置
 * <p>
 * 配置消息监听容器和消息适配器。
 * 当 Redis 频道有消息发布时，监听器会自动收到并处理。
 * <p>
 * 工作流程：
 * <pre>
 *   发布者 → Redis Channel → 监听容器 → 消息适配器 → 订阅者 Bean 方法
 * </pre>
 * <p>
 * 新增订阅者只需：
 * 1. 创建一个带有 {@code handleMessage(String message, String channel)} 方法的 Bean
 * 2. 在本配置中注册一个 {@code MessageListenerAdapter} + {@code PatternTopic}
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RedisPubSubConfig {

    private final RedisConnectionFactory connectionFactory;
    private final RoomMessageSubscriber roomMessageSubscriber;
    private final NotifySessionManager notifySessionManager;
    private final ObjectMapper objectMapper;

    /**
     * 消息监听容器
     * 管理所有订阅者的连接和线程，监听 Redis 频道的消息推送。
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer() {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // 订阅所有以 "channel:*" 开头的频道（统一的频道命名前缀）
        // 不同的业务频道通过 handleMessage 方法中的 channel 参数区分
        container.addMessageListener(
                new MessageListenerAdapter(new RedisMessageSubscriber(roomMessageSubscriber, notifySessionManager, objectMapper)),
                new PatternTopic("channel:*")
        );

        return container;
    }

    // ==================== 内部订阅者 ====================

    /**
     * 通用消息订阅者（内部类）
     * <p>
     * 实现 {@code handleMessage} 方法（由 MessageListenerAdapter 约定的方法签名），
     * 根据频道名分发到不同的业务处理器。
     * <p>
     * 歌房频道（ROOM_SYNC/ROOM_CHAT/ROOM_MEMBER）委托给 {@link RoomMessageSubscriber} 处理，
     * 该组件管理 WebSocket 会话广播。
     */
    @RequiredArgsConstructor
    public static class RedisMessageSubscriber {

        private final RoomMessageSubscriber roomMessageSubscriber;
        private final NotifySessionManager notifySessionManager;
        private final ObjectMapper objectMapper;

        /**
         * 收到消息时的回调方法
         * <p>
         * 方法名由 MessageListenerAdapter 的默认策略 {@code DEFAULT_LISTENER_METHOD = "handleMessage"} 决定。
         *
         * @param message 消息内容（JSON 字符串，由发布者序列化）
         * @param channel 频道名
         */
        public void handleMessage(String message, String channel) {
            log.debug("【Redis Sub】channel={}, message={}", channel, message);

            try {
                switch (channel) {
                    // ===== 歌房相关（委托给 RoomMessageSubscriber） =====
                    case RedisChannels.ROOM_SYNC:
                        roomMessageSubscriber.handleRoomSync(message);
                        break;
                    case RedisChannels.ROOM_CHAT:
                        roomMessageSubscriber.handleRoomChat(message);
                        break;
                    case RedisChannels.ROOM_MEMBER:
                        roomMessageSubscriber.handleRoomMember(message);
                        break;

                    // ===== 通知相关 =====
                    case RedisChannels.USER_NOTIFY:
                        handleUserNotify(message);
                        break;
                    case RedisChannels.SYSTEM_BROADCAST:
                        handleSystemBroadcast(message);
                        break;

                    // ===== 异步任务 =====
                    case RedisChannels.PLAY_COUNT_FLUSH:
                        handlePlayCountFlush(message);
                        break;

                    default:
                        log.debug("【Redis Sub】未匹配的频道: {}", channel);
                        break;
                }
            } catch (Exception e) {
                log.error("【Redis Sub】处理消息异常 channel={}, message={}", channel, message, e);
            }
        }

        // ==================== 业务处理器 ====================

        /**
         * 用户通知实时推送
         * <p>
         * 从 Redis USER_NOTIFY 频道收到通知 JSON，解析后推送
         * 到目标用户的 WebSocket 连接。
         */
        private void handleUserNotify(String message) {
            try {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> data = objectMapper.readValue(message, java.util.Map.class);
                Object userIdObj = data.get("userId");
                if (userIdObj == null) {
                    log.warn("【用户通知】缺少 userId 字段: {}", message);
                    return;
                }
                Long userId;
                if (userIdObj instanceof Number) {
                    userId = ((Number) userIdObj).longValue();
                } else {
                    userId = Long.valueOf(userIdObj.toString());
                }

                // 推送到目标用户 WebSocket
                boolean sent = notifySessionManager.sendToUser(userId, message);
                if (sent) {
                    log.debug("【用户通知】已推送到用户 {}: {}", userId, data.get("title"));
                }
            } catch (Exception e) {
                log.error("【用户通知】处理异常: {}", message, e);
            }
        }

        /**
         * 系统广播推送
         * <p>
         * 从 Redis SYSTEM_BROADCAST 频道收到广播 JSON，
         * 推送到所有在线用户的 WebSocket 连接。
         */
        private void handleSystemBroadcast(String message) {
            try {
                notifySessionManager.broadcast(message);
                log.debug("【系统广播】已广播给所有在线用户");
            } catch (Exception e) {
                log.error("【系统广播】处理异常: {}", message, e);
            }
        }

        private void handlePlayCountFlush(String message) {
            log.info("【播放计数落库】{}", message);
        }
    }
}
