package com.example.music.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志实体
 * <p>
 * 记录管理员的操作行为，用于审计和追溯。
 * 通过 AOP 切面 {@code @OperationLog} 注解自动记录。
 */
@Data
public class OperationLog {

    /** 主键 ID */
    private Long id;

    /** 操作人 ID */
    private Long operatorId;

    /** 操作类型（如 BAN_USER / DELETE_COMMENT / AUDIT_SONG 等） */
    private String action;

    /** 操作对象类型（如 USER / SONG / COMMENT / REPORT） */
    private String targetType;

    /** 操作对象 ID */
    private Long targetId;

    /** 操作详情（JSON 格式，记录请求参数和响应结果） */
    private String detail;

    /** 操作人 IP 地址 */
    private String ip;

    /** 创建时间 */
    private LocalDateTime createdTime;
}