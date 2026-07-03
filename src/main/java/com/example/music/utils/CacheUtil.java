package com.example.music.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存工具类
 * <p>
 * 对 RedisTemplate 的一层封装，提供类型安全且语义清晰的缓存操作方法。
 * 所有缓存 Key 遵循 {@code music:xxx:yyy} 的命名规范。
 * <p>
 * 使用方式：
 * <pre>{@code
 *     // 注入
 *     @Autowired private CacheUtil cacheUtil;
 *
 *     // 存 5 分钟的缓存
 *     cacheUtil.set("cache:hot_songs", songList, 300, TimeUnit.SECONDS);
 *
 *     // 取泛型
 *     List<SongVO> cached = cacheUtil.get("cache:hot_songs", List.class);
 * }</pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    // ==================== 基础操作 ====================

    /**
     * 设置缓存（无过期时间，永久有效）
     *
     * @param key   缓存键
     * @param value 缓存值
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 设置缓存（带过期时间）
     *
     * @param key      缓存键
     * @param value    缓存值
     * @param timeout  过期时长
     * @param timeUnit 时间单位
     */
    public void set(String key, Object value, long timeout, TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }

    /**
     * 获取缓存
     *
     * @param key 缓存键
     * @param <T> 期望返回值类型
     * @return 缓存值（不存在时返回 null）
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        try {
            return (T) value;
        } catch (ClassCastException e) {
            log.warn("【缓存类型转换失败】key={}, valueType={}", key, value.getClass().getName());
            return null;
        }
    }

    /**
     * 获取缓存（指定类型）
     *
     * @param key   缓存键
     * @param clazz 目标类型
     * @param <T>   类型参数
     * @return 缓存值（不存在时返回 null）
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        try {
            return (T) value;
        } catch (ClassCastException e) {
            log.warn("【缓存类型转换失败】key={}, expected={}, actual={}",
                    key, clazz.getName(), value.getClass().getName());
            return null;
        }
    }

    /**
     * 获取缓存值并原子性地删除该键（等同于 Redis GETDEL 命令）
     *
     * @param key 缓存键
     * @param <T> 期望返回值类型
     * @return 缓存值（不存在时返回 null）
     */
    @SuppressWarnings("unchecked")
    public <T> T getAndDelete(String key) {
        Object value = redisTemplate.opsForValue().getAndDelete(key);
        if (value == null) {
            return null;
        }
        try {
            return (T) value;
        } catch (ClassCastException e) {
            log.warn("【缓存类型转换失败】key={}, valueType={}", key, value.getClass().getName());
            return null;
        }
    }

    /**
     * 删除缓存
     *
     * @param key 缓存键
     * @return true=删除成功 / false=键不存在
     */
    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    /**
     * 批量删除缓存
     *
     * @param keys 缓存键集合
     * @return 成功删除的数量
     */
    public long delete(Collection<String> keys) {
        Long result = redisTemplate.delete(keys);
        return result != null ? result : 0;
    }

    /**
     * 删除匹配某个模式的所有缓存（如 "cache:lyrics:*"）
     * ⚠ 性能注意：keys 命令在大量 key 时可能阻塞 Redis，生产环境建议用 SCAN
     *
     * @param pattern 匹配模式（如 "cache:lyrics:*"）
     * @return 成功删除的数量
     */
    public long deleteByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys == null || keys.isEmpty()) {
            return 0;
        }
        return delete(keys);
    }

    // ==================== 过期与存在 ====================

    /**
     * 判断缓存键是否存在
     *
     * @param key 缓存键
     * @return true=存在
     */
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 设置过期时间
     *
     * @param key      缓存键
     * @param timeout  过期时长
     * @param timeUnit 时间单位
     * @return true=设置成功
     */
    public boolean expire(String key, long timeout, TimeUnit timeUnit) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, timeUnit));
    }

    /**
     * 获取剩余过期时间
     *
     * @param key 缓存键
     * @return 剩余秒数（-1=永不过期，-2=键不存在）
     */
    public long getTtl(String key) {
        Long ttl = redisTemplate.getExpire(key);
        return ttl != null ? ttl : -2;
    }

    // ==================== 自增/自减 ====================

    /**
     * 自增（原子操作，常用于计数）
     *
     * @param key   缓存键
     * @param delta 增量
     * @return 自增后的值
     */
    public long increment(String key, long delta) {
        Long result = redisTemplate.opsForValue().increment(key, delta);
        return result != null ? result : 0;
    }

    /**
     * 自减
     *
     * @param key   缓存键
     * @param delta 减量
     * @return 自减后的值
     */
    public long decrement(String key, long delta) {
        Long result = redisTemplate.opsForValue().decrement(key, delta);
        return result != null ? result : 0;
    }

    // ==================== Hash 操作 ====================

    /**
     * 设置 Hash 字段
     *
     * @param key   Hash 键
     * @param field 字段名
     * @param value 字段值
     */
    public void hSet(String key, String field, Object value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    /**
     * 获取 Hash 字段
     *
     * @param key   Hash 键
     * @param field 字段名
     * @param <T>   返回值类型
     * @return 字段值
     */
    @SuppressWarnings("unchecked")
    public <T> T hGet(String key, String field) {
        Object value = redisTemplate.opsForHash().get(key, field);
        try {
            return (T) value;
        } catch (ClassCastException e) {
            return null;
        }
    }

    /**
     * 获取整个 Hash
     *
     * @param key Hash 键
     * @param <T> 值类型
     * @return 字段名 → 字段值的 Map
     */
    public <T> Map<String, T> hGetAll(String key) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        if (entries.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, T> result = new HashMap<>(entries.size());
        entries.forEach((k, v) -> {
            try {
                @SuppressWarnings("unchecked")
                T value = (T) v;
                result.put(String.valueOf(k), value);
            } catch (ClassCastException e) {
                result.put(String.valueOf(k), null);
            }
        });
        return result;
    }

    /**
     * 删除 Hash 字段
     *
     * @param key    Hash 键
     * @param fields 字段名（可变参数）
     * @return 成功删除的字段数
     */
    public long hDelete(String key, String... fields) {
        Long result = redisTemplate.opsForHash().delete(key, (Object[]) fields);
        return result != null ? result : 0;
    }

    // ==================== Set 操作 ====================

    /**
     * 向 Set 添加元素
     *
     * @param key    Set 键
     * @param values 元素（可变参数）
     * @return 成功添加的数量
     */
    public long sAdd(String key, Object... values) {
        Long result = redisTemplate.opsForSet().add(key, values);
        return result != null ? result : 0;
    }

    /**
     * 获取 Set 所有成员
     *
     * @param key Set 键
     * @param <T> 元素类型
     * @return 成员集合
     */
    @SuppressWarnings("unchecked")
    public <T> Set<T> sMembers(String key) {
        Set<Object> members = redisTemplate.opsForSet().members(key);
        if (members == null || members.isEmpty()) {
            return Collections.emptySet();
        }
        Set<T> result = new HashSet<>(members.size());
        members.forEach(m -> {
            try {
                result.add((T) m);
            } catch (ClassCastException ignored) {
            }
        });
        return result;
    }

    /**
     * 判断元素是否在 Set 中
     *
     * @param key   Set 键
     * @param value 元素
     * @return true=存在
     */
    public boolean sIsMember(String key, Object value) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, value));
    }

    // ==================== ZSet 操作 ====================

    /**
     * 向有序集合添加元素（带分数，可用于排行榜）
     *
     * @param key   ZSet 键
     * @param value 元素
     * @param score 分数
     * @return true=添加成功
     */
    public boolean zAdd(String key, Object value, double score) {
        return Boolean.TRUE.equals(redisTemplate.opsForZSet().add(key, value, score));
    }

    /**
     * 获取有序集合排名（从高到低）
     *
     * @param key   ZSet 键
     * @param value 元素
     * @return 排名（0 开始，-1=不存在）
     */
    public long zReverseRank(String key, Object value) {
        Long rank = redisTemplate.opsForZSet().reverseRank(key, value);
        return rank != null ? rank : -1;
    }

    /**
     * 有序集合分数自增（原子操作，用于排行榜）
     *
     * @param key   ZSet 键
     * @param value 元素
     * @param delta 增量
     * @return 自增后的分数
     */
    public Double zIncrementScore(String key, Object value, double delta) {
        return redisTemplate.opsForZSet().incrementScore(key, value, delta);
    }

    /**
     * 获取有序集合指定范围成员（从高到低）
     *
     * @param key   ZSet 键
     * @param start 起始索引（0 开始）
     * @param end   结束索引（-1 表示全部）
     * @return 成员集合
     */
    public Set<Object> zReverseRange(String key, long start, long end) {
        return redisTemplate.opsForZSet().reverseRange(key, start, end);
    }

    /**
     * 获取有序集合指定范围成员及分数（从高到低）
     *
     * @param key   ZSet 键
     * @param start 起始索引（0 开始）
     * @param end   结束索引（-1 表示全部）
     * @return 带分数的成员集合
     */
    public Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<Object>> zReverseRangeWithScores(
            String key, long start, long end) {
        return redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);
    }

    /**
     * 设置 ZSet 过期时间
     *
     * @param key      ZSet 键
     * @param timeout  过期时长
     * @param timeUnit 时间单位
     * @return true=设置成功
     */
    public boolean zExpire(String key, long timeout, TimeUnit timeUnit) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, timeUnit));
    }

    // ==================== 缓存预热辅助 ====================

    /**
     * 尝试获取缓存，如果不存在则通过 supplier 加载并缓存
     * <p>
     * 典型的 Cache-Aside 模式：先从缓存读，没有则查 DB 再回写缓存。
     *
     * @param key       缓存键
     * @param clazz     返回值类型
     * @param supplier  数据加载函数（当缓存不存在时调用）
     * @param timeout   缓存过期时间
     * @param timeUnit  时间单位
     * @param <T>       返回值类型
     * @return 数据（来自缓存或 supplier）
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrLoad(String key, Class<T> clazz, java.util.function.Supplier<T> supplier,
                           long timeout, TimeUnit timeUnit) {
        // 1. 尝试从缓存取
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            try {
                return (T) cached;
            } catch (ClassCastException e) {
                log.warn("【缓存类型不匹配】key={}, 将重新加载", key);
            }
        }

        // 2. 缓存不存在或类型不匹配，加载数据
        T data = supplier.get();
        if (data != null) {
            set(key, data, timeout, timeUnit);
        }
        return data;
    }
}