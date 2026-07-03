package com.example.music.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 轮播图实体
 * <p>
 * 首页顶部轮播展示，支持排序、启用/禁用。
 * 管理员通过后台管理接口进行 CRUD 操作。
 */
@Data
public class Banner {

    /** 主键 ID */
    private Long id;

    /** 轮播图图片 URL */
    private String imageUrl;

    /** 点击跳转链接 */
    private String linkUrl;

    /** 排序序号（越小越靠前） */
    private Integer sortOrder;

    /** 是否启用（true=显示，false=隐藏） */
    private Boolean isActive;

    /** 标题（alt 文本） */
    private String title;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 更新时间 */
    private LocalDateTime updatedTime;
}