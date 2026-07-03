package com.example.music.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分类实体
 * <p>
 * 对应数据库表 {@code category}，用于歌曲的多维度分类体系：
 * - GENRE（风格）：流行、摇滚、古典等
 * - LANGUAGE（语种）：中文、英文、日文等
 * - YEAR（年代）：80年代、90年代等
 */
@Data
public class Category {

    /** 主键 ID */
    private Long id;

    /** 分类名称 */
    private String name;

    /** 分类类型: GENRE / LANGUAGE / YEAR */
    private String type;

    /** 排序序号 */
    private Integer sortOrder;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 更新时间 */
    private LocalDateTime updatedTime;
}