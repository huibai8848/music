package com.example.music.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 喜欢（红心）实体
 * <p>
 * 与收藏语义不同：喜欢是轻量级的红心标记，计入推荐权重。
 * 可喜欢类型：SONG / PLAYLIST / COMMENT。
 */
@Data
public class Likes {

    /** 主键 ID */
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 目标类型: SONG / PLAYLIST / COMMENT */
    private String targetType;

    /** 目标 ID */
    private Long targetId;

    /** 创建时间 */
    private LocalDateTime createdTime;
}