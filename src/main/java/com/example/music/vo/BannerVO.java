package com.example.music.vo;

import com.example.music.entity.Banner;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 轮播图展示 VO
 */
@Data
public class BannerVO {

    private Long id;
    private String imageUrl;
    private String linkUrl;
    private Integer sortOrder;
    private Boolean isActive;
    private String title;
    private LocalDateTime createdTime;

    /**
     * 从 Banner 实体转换
     */
    public static BannerVO fromEntity(Banner banner) {
        if (banner == null) return null;
        BannerVO vo = new BannerVO();
        vo.setId(banner.getId());
        vo.setImageUrl(banner.getImageUrl());
        vo.setLinkUrl(banner.getLinkUrl());
        vo.setSortOrder(banner.getSortOrder());
        vo.setIsActive(banner.getIsActive());
        vo.setTitle(banner.getTitle());
        vo.setCreatedTime(banner.getCreatedTime());
        return vo;
    }
}
