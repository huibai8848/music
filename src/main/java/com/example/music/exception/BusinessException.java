package com.example.music.exception;

import com.example.music.constant.ErrorCode;
import lombok.Getter;

/**
 * 业务异常
 * <p>
 * 在 Service 层抛出，由 {@link GlobalExceptionHandler} 统一捕获并转换为标准响应体。
 * 例如：throw new BusinessException(ErrorCode.USER_NOT_FOUND);
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 错误码 */
    private final ErrorCode errorCode;

    /** 额外的错误详情（可选，用于携带校验失败的具体字段信息） */
    private final Object detail;

    /**
     * @param errorCode 错误码枚举
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.detail = null;
    }

    /**
     * @param errorCode 错误码枚举
     * @param message   自定义错误描述（覆盖枚举默认消息）
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.detail = null;
    }

    /**
     * @param errorCode 错误码枚举
     * @param detail    附加错误数据（如字段校验失败明细列表）
     */
    public BusinessException(ErrorCode errorCode, Object detail) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.detail = detail;
    }

    /**
     * @param errorCode 错误码枚举
     * @param message   自定义错误描述
     * @param detail    附加错误数据
     */
    public BusinessException(ErrorCode errorCode, String message, Object detail) {
        super(message);
        this.errorCode = errorCode;
        this.detail = detail;
    }

    /**
     * @param errorCode 错误码枚举
     * @param cause     原始异常（保留堆栈链）
     */
    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.detail = null;
    }
}