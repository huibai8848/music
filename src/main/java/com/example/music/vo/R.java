package com.example.music.vo;

import lombok.Data;

import java.time.Instant;

/**
 * 统一响应体
 * <p>
 * 所有 API 接口统一使用该结构返回，保证前后端交互格式一致。
 * 前端 axios 拦截器统一根据 code 判断请求是否成功。
 *
 * @param <T> 响应数据类型
 */
@Data
public class R<T> {

    /** 业务状态码（200 成功，非 200 失败） */
    private int code;

    /** 提示消息 */
    private String message;

    /** 响应数据 */
    private T data;

    /** 响应时间戳（毫秒） */
    private long timestamp;

    // ==================== 构造方法 ====================

    private R() {
        this.timestamp = Instant.now().toEpochMilli();
    }

    private R(int code, String message, T data) {
        this();
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // ==================== 成功响应 ====================

    /**
     * 成功 —— 无数据返回
     */
    public static <T> R<T> ok() {
        return new R<>(200, "操作成功", null);
    }

    /**
     * 成功 —— 带数据返回
     *
     * @param data 响应数据
     */
    public static <T> R<T> ok(T data) {
        return new R<>(200, "操作成功", data);
    }

    /**
     * 成功 —— 自定义消息 + 数据
     *
     * @param message 提示消息
     * @param data    响应数据
     */
    public static <T> R<T> ok(String message, T data) {
        return new R<>(200, message, data);
    }

    // ==================== 失败响应 ====================

    /**
     * 失败 —— 仅返回错误码和消息
     *
     * @param code    业务状态码
     * @param message 错误描述
     */
    public static <T> R<T> fail(int code, String message) {
        return new R<>(code, message, null);
    }

    /**
     * 失败 —— 带错误数据的返回
     *
     * @param code    业务状态码
     * @param message 错误描述
     * @param data    附加错误数据（如校验失败字段明细）
     */
    public static <T> R<T> fail(int code, String message, T data) {
        return new R<>(code, message, data);
    }

    /**
     * 失败 —— 便捷方法，使用默认 400 状态码
     *
     * @param message 错误描述
     */
    public static <T> R<T> fail(String message) {
        return new R<>(400, message, null);
    }

    // ==================== 链式便捷方法 ====================

    /**
     * 链式设置消息
     */
    public R<T> message(String message) {
        this.message = message;
        return this;
    }

    /**
     * 链式设置数据
     */
    public R<T> data(T data) {
        this.data = data;
        return this;
    }
}