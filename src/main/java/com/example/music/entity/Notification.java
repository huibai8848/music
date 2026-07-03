package com.example.music.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知实体
 * <p>
 * 记录用户收到的通知消息，包括评论回复、系统公告、会员到期提醒、举报处理结果。
 * 支持实时推送（WebSocket）和离线查询（未读角标）。
 */
@Data
public class Notification {

    /** 主键 ID */
    private Long id;

    /** 接收者 ID */
    private Long userId;

    /**
     * 通知类型
     * <ul>
     *   <li>REPLY — 评论回复</li>
     *   <li>SYSTEM — 系统公告</li>
     *   <li>MEMBERSHIP — 会员到期提醒</li>
     *   <li>REPORT_RESULT — 举报处理结果</li>
     *   <li>SONG_REVIEW — 歌曲审核提醒</li>
     * </ul>
     */
    private String type;

    /** 通知标题 */
    private String title;

    /** 通知内容 */
    private String content;

    /** 是否已读（0=未读，1=已读） */
    private Boolean isRead;

    /** 关联类型（如 COMMENT / SONG 等） */
    private String relatedType;

    /** 关联 ID */
    private Long relatedId;

    /** 创建时间 */
    private LocalDateTime createdTime;
}