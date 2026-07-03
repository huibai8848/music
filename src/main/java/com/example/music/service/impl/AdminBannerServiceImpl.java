package com.example.music.service.impl;

import com.example.music.dto.BannerDTO;
import com.example.music.entity.Banner;
import com.example.music.mapper.BannerMapper;
import com.example.music.service.AdminBannerService;
import com.example.music.vo.BannerVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 轮播图管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminBannerServiceImpl implements AdminBannerService {

    private final BannerMapper bannerMapper;

    @Override
    public List<BannerVO> listBanners() {
        return bannerMapper.selectList().stream()
                .map(BannerVO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<BannerVO> getActiveBanners() {
        return bannerMapper.selectActive().stream()
                .map(BannerVO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public BannerVO getBanner(Long id) {
        Banner banner = bannerMapper.selectById(id);
        return BannerVO.fromEntity(banner);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BannerVO createBanner(BannerDTO dto) {
        Banner banner = new Banner();
        banner.setImageUrl(dto.getImageUrl());
        banner.setLinkUrl(dto.getLinkUrl());
        banner.setSortOrder(dto.getSortOrder());
        banner.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        banner.setTitle(dto.getTitle());

        bannerMapper.insert(banner);
        log.info("新增轮播图: id={}, title={}", banner.getId(), banner.getTitle());
        return BannerVO.fromEntity(banner);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BannerVO updateBanner(Long id, BannerDTO dto) {
        Banner banner = bannerMapper.selectById(id);
        if (banner == null) return null;

        if (dto.getImageUrl() != null) banner.setImageUrl(dto.getImageUrl());
        if (dto.getLinkUrl() != null) banner.setLinkUrl(dto.getLinkUrl());
        if (dto.getSortOrder() != null) banner.setSortOrder(dto.getSortOrder());
        if (dto.getIsActive() != null) banner.setIsActive(dto.getIsActive());
        if (dto.getTitle() != null) banner.setTitle(dto.getTitle());

        bannerMapper.update(banner);
        log.info("更新轮播图: id={}, title={}", id, banner.getTitle());
        return BannerVO.fromEntity(banner);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBanner(Long id) {
        bannerMapper.deleteById(id);
        log.info("删除轮播图: id={}", id);
    }
}
