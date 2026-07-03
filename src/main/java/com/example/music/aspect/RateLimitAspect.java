package com.example.music.aspect;

import com.example.music.constant.ErrorCode;
import com.example.music.constant.RateLimitScript;
import com.example.music.constant.RedisKeys;
import com.example.music.exception.BusinessException;
import com.example.music.utils.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.List;

/**
 * 接口限流切面
 * <p>
 * 拦截所有标注了 {@link RateLimit} 的 Controller 方法，
 * 通过执行 Redis Lua 脚本实现滑动窗口计数，超过限制则抛出 TOO_MANY_REQUESTS 异常。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    // 使用 StringRedisTemplate 执行 Lua 脚本，避免 GenericJackson2JsonRedisSerializer 序列化参数为 JSON 字符串
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 环绕通知：执行前校验限流，通过后才执行业务逻辑
     */
    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        // 1. 获取限流参数
        int limit = rateLimit.count();
        int expireTime = rateLimit.time();

        // 2. 构建限流 Key：ratelimit:{userId|IP}:{自定义key}:{URI}
        String keySuffix = buildKeySuffix(rateLimit.key());
        String rateLimitKey = RedisKeys.RATE_LIMIT + keySuffix;

        // 3. 执行 Lua 脚本（使用 String 类型参数）
        DefaultRedisScript<List> script = new DefaultRedisScript<>(RateLimitScript.SCRIPT, List.class);
        List<Object> result = stringRedisTemplate.execute(
                script,
                Arrays.asList(rateLimitKey),
                String.valueOf(limit),
                String.valueOf(expireTime)
        );

        if (result != null && result.size() >= 2) {
            long passed = Long.parseLong(String.valueOf(result.get(0)));
            long ttl = Long.parseLong(String.valueOf(result.get(1)));

            if (passed == 0) {
                log.warn("【限流拦截】key={}, limit={}/{}s, ttl={}ms", rateLimitKey, limit, expireTime, ttl);
                throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
            }
        }

        // 4. 未超限，执行业务逻辑
        return joinPoint.proceed();
    }

    /**
     * 构建限流 Key 后缀
     * 优先级：用户 ID > 自定义 Key > IP 地址
     */
    private String buildKeySuffix(String customKey) {
        if (!customKey.isEmpty()) {
            return customKey;
        }
        Long userId = RequestContext.getUserId();
        if (userId != null) {
            return "user:" + userId;
        }
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            return "ip:" + getClientIp(request);
        }
        return "unknown";
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return null;
        return attrs.getRequest();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
