package com.example.music.aspect;

import java.lang.annotation.*;

/**
 * 接口限流注解
 * <p>
 * 基于 Redis + Lua 脚本的滑动窗口限流实现。
 * 标注在 Controller 方法上即可生效。
 * <p>
 * 使用示例：
 * <pre>{@code
 *     @RateLimit(count = 10, time = 60)  // 每分钟最多 10 次
 *     @PostMapping("/api/auth/login")
 *     public R<?> login(@RequestBody LoginDTO dto) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * @return 限制次数（时间窗口内允许的最大请求数）
     */
    int count() default 60;

    /**
     * @return 时间窗口（秒）
     */
    int time() default 60;

    /**
     * @return 限流 Key 后缀（可选，默认为 userId:URI）
     *         <p>用于自定义限流粒度，如 "login:" + IP 地址（登录接口按 IP 限流）
     */
    String key() default "";
}