package com.example.music.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统公告实体
 * <p>
 * 管理员发布的系统级公告，支持类型区分（系统通知/维护公告/活动公告）。
 * 前端可在首页或公告页面展示。
 */
@Data
public class SystemNotice {

    /** 主键 ID */
    private Long id;

    /** 公告标题 */
    private String title;

    /** 公告内容 */
    private String content;

    /** 公告类型: SYSTEM / MAINTENANCE / ACTIVITY */
    private String type;

    /** 是否启用（true=显示，false=隐藏） */
    private Boolean isActive;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 更新时间 */
    private LocalDateTime updatedTime;
}