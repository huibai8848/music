package com.example.music.aspect;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * <p>
 * 标注在 Controller 方法上，通过 AOP 切面 {@link OperationLogAspect} 自动记录操作日志。
 * 日志记录内容包括：操作人、操作类型、目标对象、请求参数、响应结果、IP 地址。
 * <p>
 * 使用示例：
 * <pre>{@code
 * @OperationLog("封禁用户")
 * @PutMapping("/api/admin/users/{id}/ban")
 * public R<Void> banUser(@PathVariable Long id) { ... }
 * }</pre>
 *
 * @see OperationLogAspect
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /**
     * 操作描述
     * <p>
     * 简要描述该操作，如"封禁用户"、"删除歌曲"、"处理举报"。
     * 该值会记录到 operation_log 表的 action 字段。
     */
    String value() default "";

    /**
     * 操作对象类型
     * <p>
     * 如 USER / SONG / COMMENT / REPORT 等。
     * 留空时由切面自动从方法名或参数推断。
     */
    String targetType() default "";
}
