package com.example.music.exception;

import com.example.music.constant.ErrorCode;
import com.example.music.vo.R;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * <p>
 * 通过 {@code @RestControllerAdvice} 统一捕获所有 Controller 层抛出的异常，
 * 转换为 {@link R} 标准响应体返回前端，避免直接暴露异常堆栈。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常 —— 由 Service 层主动抛出
     * <p>
     * 例如：throw new BusinessException(ErrorCode.USER_NOT_FOUND);
     */
    @ExceptionHandler(BusinessException.class)
    public R<Object> handleBusinessException(BusinessException e) {
        // 记录警告日志（业务异常属于预期行为，非系统错误）
        log.warn("【业务异常】code={}, message={}", e.getErrorCode().getCode(), e.getMessage());
        return R.fail(e.getErrorCode().getCode(), e.getMessage(), e.getDetail());
    }

    /**
     * 请求参数校验失败（@Valid + @RequestBody）
     * <p>
     * 例如：实体类字段上的 @NotBlank/@Size 校验不通过时自动触发
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Object> handleValidationException(MethodArgumentNotValidException e) {
        log.warn("【参数校验失败】{}", e.getMessage());

        // 收集所有校验失败的字段及错误消息
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();

        // 取第一条错误消息作为提示
        String firstMessage = fieldErrors.stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("请求参数校验失败");

        return R.fail(ErrorCode.PARAM_INVALID.getCode(), firstMessage, fieldErrors);
    }

    /**
     * 请求参数校验失败（@RequestParam + @Valid 或 @PathVariable 校验）
     * <p>
     * 例如：方法参数上的 @NotBlank/@Min 校验不通过
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public R<Object> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("【参数校验失败】{}", e.getMessage());

        List<String> messages = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());

        return R.fail(ErrorCode.PARAM_INVALID.getCode(),
                messages.isEmpty() ? "请求参数校验失败" : messages.get(0),
                messages);
    }

    /**
     * 请求体格式错误（如 JSON 语法错误、类型不匹配）
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<Object> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("【请求体格式错误】{}", e.getMessage());
        return R.fail(ErrorCode.BAD_REQUEST.getCode(), "请求数据格式错误");
    }

    /**
     * 缺少必需的请求参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public R<Object> handleMissingParamException(MissingServletRequestParameterException e) {
        log.warn("【缺少请求参数】{}", e.getMessage());
        return R.fail(ErrorCode.BAD_REQUEST.getCode(), "缺少必需的参数: " + e.getParameterName());
    }

    /**
     * 请求参数类型转换错误
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public R<Object> handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("【参数类型错误】{}", e.getMessage());
        return R.fail(ErrorCode.BAD_REQUEST.getCode(),
                "参数 " + e.getName() + " 类型不匹配，期望类型: " + e.getRequiredType().getSimpleName());
    }

    /**
     * 请求方法不支持（如 GET 请求调用了 POST 接口）
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public R<Object> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("【请求方法不支持】{}", e.getMessage());
        return R.fail(ErrorCode.METHOD_NOT_ALLOWED.getCode(), "请求方法不支持: " + e.getMethod());
    }

    /**
     * 文件上传大小超过 Spring 限制
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public R<Object> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("【文件上传超限】{}", e.getMessage());
        return R.fail(ErrorCode.FILE_SIZE_EXCEED.getCode(), "上传文件大小超过限制");
    }

    /**
     * 缺少必需的 multipart 请求部分（如 @RequestPart 对应的文件未上传）
     * <p>
     * 当 multipart 请求中缺少 @RequestPart("audio") 等必需的文件部分时触发。
     * 注意与 @RequestParam 的参数缺失区分（后者抛出 MissingServletRequestParameterException）。
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public R<Object> handleMissingServletRequestPartException(MissingServletRequestPartException e) {
        log.warn("【缺少 multipart 请求部分】{}", e.getMessage());
        return R.fail(ErrorCode.BAD_REQUEST.getCode(), "缺少必需的文件参数: " + e.getRequestPartName());
    }

    /**
     * multipart 请求解析失败（如格式错误、边界缺失、上传中断）
     * <p>
     * 当客户端发送的 multipart/form-data 请求无法被 Spring 正确解析时触发，
     * 例如：请求体被截断、boundary 格式错误、或上传过程中连接断开。
     */
    @ExceptionHandler(MultipartException.class)
    public R<Object> handleMultipartException(MultipartException e) {
        log.warn("【multipart 请求解析失败】{}", e.getMessage());
        return R.fail(ErrorCode.BAD_REQUEST.getCode(), "文件上传格式错误，请重新选择文件");
    }

    /**
     * 数据库异常（MyBatis/MySQL 操作失败）
     */
    @ExceptionHandler(DataAccessException.class)
    public R<Object> handleDataAccessException(DataAccessException e) {
        log.error("【数据库异常】{}", e.getMessage(), e);
        return R.fail(ErrorCode.INTERNAL_ERROR.getCode(), "数据处理失败，请稍后重试");
    }

    /**
     * 静态资源/路由 404
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public R<Object> handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("【资源不存在】{}", e.getMessage());
        return R.fail(ErrorCode.NOT_FOUND.getCode(), "请求的资源不存在");
    }

    /**
     * 兜底异常 —— 所有未处理的异常最终由这里捕获
     * <p>
     * ⚠ 这里需要记录完整的堆栈信息以便排查
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Object> handleException(Exception e) {
        // 记录完整错误堆栈（严重错误，需要人工介入修复）
        log.error("【系统异常】{}", e.getMessage(), e);
        return R.fail(ErrorCode.INTERNAL_ERROR.getCode(), "服务器繁忙，请稍后再试");
    }
}