package com.example.music.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 举报实体
 * <p>
 * 用户可举报评论、歌曲、歌单等不当内容。
 * 管理员后台处理举报，处理结果通知举报人。
 * 同一用户 24 小时内对同一内容不可重复举报。
 */
@Data
public class Report {

    /** 主键 ID */
    private Long id;

    /** 举报人 ID */
    private Long reporterId;

    /** 目标类型: COMMENT / SONG / PLAYLIST */
    private String targetType;

    /** 目标 ID */
    private Long targetId;

    /** 举报原因: PORNOGRAPHY / AD / ABUSE / COPYRIGHT / OTHER */
    private String reason;

    /** 补充说明（最多 500 字） */
    private String description;

    /** 处理状态: PENDING / RESOLVED / DISMISSED */
    private String status;

    /** 处理人 ID */
    private Long handlerId;

    /** 处理备注 */
    private String handleNote;

    /** 处理时间 */
    private LocalDateTime handledTime;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 更新时间 */
    private LocalDateTime updatedTime;
}