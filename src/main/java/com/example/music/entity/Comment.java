package com.example.music.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论实体
 * <p>
 * 支持多态关联（可评论歌曲和歌单），
 * 支持楼中楼回复（通过 parentId 关联），
 * 支持软删除（status = DELETED）。
 */
@Data
public class Comment {

    /** 主键 ID */
    private Long id;

    /** 评论者 ID */
    private Long userId;

    /** 目标类型: SONG / PLAYLIST */
    private String targetType;

    /** 目标 ID */
    private Long targetId;

    /** 评论内容（已 HTML 转义） */
    private String content;

    /** 回复的评论 ID（null = 一级评论） */
    private Long parentId;

    /** 状态: VISIBLE / DELETED */
    private String status;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 更新时间 */
    private LocalDateTime updatedTime;
}