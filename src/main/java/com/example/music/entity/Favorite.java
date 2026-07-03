package com.example.music.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 收藏实体
 * <p>
 * 多态关联设计：targetType + targetId 指向被收藏的对象。
 * 可收藏类型：SONG / PLAYLIST / ALBUM。
 */
@Data
public class Favorite {

    /** 主键 ID */
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 目标类型: SONG / PLAYLIST / ALBUM */
    private String targetType;

    /** 目标 ID */
    private Long targetId;

    /** 创建时间 */
    private LocalDateTime createdTime;
}