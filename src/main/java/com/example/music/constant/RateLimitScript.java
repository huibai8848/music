package com.example.music.constant;

/**
 * Redis Lua 限流脚本
 * <p>
 * 原子执行：INCR → 判断是否首次 → 设置 TTL → 返回当前计数
 * 不存在竞态条件：
 *   - Redis 是单线程执行 Lua 脚本
 *   - INCR 和 EXPIRE 在同一脚本中连续执行，不会被其他命令插入
 */
public interface RateLimitScript {

    String SCRIPT =
            "local key = KEYS[1]\n" +
            "local limit = tonumber(ARGV[1])\n" +
            "local expireTime = tonumber(ARGV[2])\n" +
            "\n" +
            "-- INCR 返回自增后的值\n" +
            "local current = redis.call('INCR', key)\n" +
            "\n" +
            "-- 第一次访问时设置过期时间\n" +
            "if current == 1 then\n" +
            "    redis.call('EXPIRE', key, expireTime)\n" +
            "end\n" +
            "\n" +
            "-- 判断是否超过限制\n" +
            "if current > limit then\n" +
            "    -- 返回 TTL（毫秒），调用方可用作 Retry-After 头\n" +
            "    local ttl = redis.call('PTTL', key)\n" +
            "    return {0, ttl}\n" +
            "end\n" +
            "\n" +
            "return {1, current}";
}