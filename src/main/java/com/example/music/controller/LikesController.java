package com.example.music.controller;

import com.example.music.service.LikesService;
import com.example.music.utils.RequestContext;
import com.example.music.vo.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 喜欢（红心）控制器
 * <p>
 * 轻量级的喜欢标记，用于给歌曲、评论等点赞。
 */
@RestController
@RequestMapping("/api/likes")
@RequiredArgsConstructor
public class LikesController {

    private final LikesService likesService;

    /**
     * 喜欢
     *
     * @param type 目标类型: song / playlist / comment
     * @param id   目标 ID
     */
    @PostMapping("/{type}/{id}")
    public R<Object> addLike(@PathVariable String type, @PathVariable Long id) {
        Long userId = RequestContext.getUserId();
        likesService.addLike(userId, type, id);
        return R.ok();
    }

    /**
     * 取消喜欢
     */
    @DeleteMapping("/{type}/{id}")
    public R<Object> removeLike(@PathVariable String type, @PathVariable Long id) {
        Long userId = RequestContext.getUserId();
        likesService.removeLike(userId, type, id);
        return R.ok();
    }

    /**
     * 查询喜欢状态和数量
     */
    @GetMapping("/{type}/{id}")
    public R<Map<String, Object>> getLikeStatus(@PathVariable String type, @PathVariable Long id) {
        long count = likesService.countLikes(type, id);
        boolean liked = false;
        if (RequestContext.isLoggedIn()) {
            liked = likesService.isLiked(RequestContext.getUserId(), type, id);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        result.put("liked", liked);
        return R.ok(result);
    }
}
