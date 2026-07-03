package com.example.music.service;

import com.example.music.dto.BannerDTO;
import com.example.music.vo.BannerVO;

import java.util.List;

/**
 * 管理后台轮播图服务接口
 * <p>
 * 提供轮播图的 CRUD 管理功能。
 */
public interface AdminBannerService {

    /** 获取所有轮播图（管理端） */
    List<BannerVO> listBanners();

    /** 获取所有已启用的轮播图（前台，按排序顺序） */
    List<BannerVO> getActiveBanners();

    /** 获取单个轮播图 */
    BannerVO getBanner(Long id);

    /** 新增轮播图 */
    BannerVO createBanner(BannerDTO dto);

    /** 更新轮播图 */
    BannerVO updateBanner(Long id, BannerDTO dto);

    /** 删除轮播图 */
    void deleteBanner(Long id);
}
