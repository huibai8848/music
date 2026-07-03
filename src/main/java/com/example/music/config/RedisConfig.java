package com.example.music.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置类
 *
 * 功能说明：
 * 1. 配置 RedisTemplate 的序列化方式（Key 使用 String，Value 使用 JSON）
 * 2. 解决 Redis 存储中文乱码和对象序列化问题
 * 3. 注册 JavaTimeModule 使 LocalDateTime 正确序列化/反序列化
 *
 * Key 命名规范（所有模块统一遵守）：
 *   bl_token:{jti}              — Token 黑名单
 *   cache:hot_songs             — 热门歌曲缓存
 *   cache:lyrics:{songId}       — 歌词缓存
 *   room:{roomId}               — 歌房状态 (Hash)
 *   room:{roomId}:members       — 歌房成员 (Set)
 *   ratelimit:{userId}:{uri}    — API 限流
 *   online:{userId}             — 在线状态
 *   play_count:buffer:{songId}  — 播放计数缓冲
 *   progress:{userId}:{songId}  — 播放进度
 */
@Configuration
public class RedisConfig {

    /**
     * RedisTemplate Bean
     * 使用 Lettuce 连接工厂（Spring Boot 3.x 默认已配置）
     *
     * @param connectionFactory Lettuce 连接工厂
     * @return 配置好的 RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // ---------- JSON 序列化器（用于 Value） ----------
        // GenericJackson2JsonRedisSerializer 是 Spring Data Redis 3.x 推荐的通用 JSON 序列化器，
        // 自动在 JSON 中写入 @class 类型信息，反序列化时能还原为正确类型。
        // 同时支持 Jackson 注解和 Java 8 时间类型。
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        mapper.registerModule(new JavaTimeModule());

        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(mapper);

        // ---------- String 序列化器（用于 Key） ----------
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // ---------- 配置序列化方式 ----------
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();

        return template;
    }
}