package com.example.music.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 轮播图请求 DTO
 * <p>
 * 管理员创建/更新轮播图时使用。
 */
@Data
public class BannerDTO {

    /** 轮播图图片 URL */
    @NotBlank(message = "图片地址不能为空")
    private String imageUrl;

    /** 点击跳转链接 */
    private String linkUrl;

    /** 排序序号 */
    @NotNull(message = "排序序号不能为空")
    private Integer sortOrder;

    /** 是否启用 */
    private Boolean isActive;

    /** 标题 */
    private String title;
}