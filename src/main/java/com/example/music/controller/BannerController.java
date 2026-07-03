package com.example.music.controller;

import com.example.music.service.AdminBannerService;
import com.example.music.vo.BannerVO;
import com.example.music.vo.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 轮播图公开控制器
 * <p>
 * 提供前台首页轮播图的读取接口，无需登录。
 * 管理端 CRUD 在 {@link AdminController} 中。
 */
@RestController
@RequestMapping("/api/banners")
@RequiredArgsConstructor
public class BannerController {

    private final AdminBannerService bannerService;

    /**
     * 获取所有已启用的轮播图（按排序顺序）
     * <p>
     * 该接口在白名单中，无需登录即可访问。
     */
    @GetMapping
    public R<List<BannerVO>> getActiveBanners() {
        return R.ok(bannerService.getActiveBanners());
    }
}