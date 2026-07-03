package com.example.music.controller;

import com.example.music.service.FavoriteService;
import com.example.music.utils.RequestContext;
import com.example.music.vo.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 收藏控制器
 * <p>
 * 收藏/取消收藏歌曲、歌单、专辑。
 */
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    /**
     * 收藏
     *
     * @param type 目标类型: song / playlist / album
     * @param id   目标 ID
     */
    @PostMapping("/{type}/{id}")
    public R<Object> addFavorite(@PathVariable String type, @PathVariable Long id) {
        Long userId = RequestContext.getUserId();
        favoriteService.addFavorite(userId, type, id);
        return R.ok("收藏成功");
    }

    /**
     * 取消收藏
     */
    @DeleteMapping("/{type}/{id}")
    public R<Object> removeFavorite(@PathVariable String type, @PathVariable Long id) {
        Long userId = RequestContext.getUserId();
        favoriteService.removeFavorite(userId, type, id);
        return R.ok("已取消收藏");
    }

    /**
     * 我的收藏列表
     */
    @GetMapping
    public R<Map<String, Object>> listFavorites(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = RequestContext.getUserId();
        return R.ok(favoriteService.listFavorites(userId, page, size));
    }
}
