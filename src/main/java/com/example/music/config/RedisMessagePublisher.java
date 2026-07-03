package com.example.music.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis 消息发布者
 * <p>
 * 封装 RedisTemplate 的 convertAndSend 方法，
 * 用于向指定频道发布消息，供其他服务实例或同一实例的订阅者消费。
 * <p>
 * 使用示例：
 * <pre>{@code
 *     // 发布歌房同步消息
 *     RoomSyncMessage msg = new RoomSyncMessage(roomId, "play", songId, progress);
 *     redisPublisher.publish(RedisChannels.ROOM_SYNC, msg);
 *
 *     // 发布通知
 *     redisPublisher.publish(RedisChannels.USER_NOTIFY, notifyMsg);
 * }</pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisMessagePublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 发布消息到指定频道
     * <p>
     * 消息体使用 RedisTemplate 配置的 JSON 序列化器自动序列化。
     *
     * @param channel 频道名（参见 {@link com.example.music.constant.RedisChannels}）
     * @param message 消息对象（会被序列化为 JSON）
     */
    public void publish(String channel, Object message) {
        log.debug("【Redis Pub】channel={}, message={}", channel, message);
        redisTemplate.convertAndSend(channel, message);
    }
}
