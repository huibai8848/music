package com.example.music.controller;

import com.example.music.service.PlayHistoryService;
import com.example.music.utils.RequestContext;
import com.example.music.vo.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 播放历史控制器
 * <p>
 * 记录用户每次切换歌曲的行为，支持查看和清除最近播放记录。
 */
@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class PlayHistoryController {

    private final PlayHistoryService playHistoryService;

    /**
     * 获取最近播放记录
     * <p>
     * 按播放时间降序返回，最多 100 条。每条包含歌曲信息和播放时间。
     */
    @GetMapping
    public R<List<Map<String, Object>>> getHistory() {
        Long userId = RequestContext.getUserId();
        return R.ok(playHistoryService.getHistory(userId));
    }

    /**
     * 记录播放历史
     * <p>
     * 前端每次切换歌曲时调用，传入 songId。
     */
    @PostMapping
    public R<Object> recordHistory(@RequestBody Map<String, Long> body) {
        Long userId = RequestContext.getUserId();
        Long songId = body.get("songId");
        if (songId == null) {
            return R.fail("缺少 songId");
        }
        playHistoryService.recordPlay(userId, songId);
        return R.ok();
    }

    /**
     * 清除播放记录
     */
    @DeleteMapping
    public R<Object> clearHistory() {
        Long userId = RequestContext.getUserId();
        playHistoryService.clearHistory(userId);
        return R.ok("播放记录已清除");
    }
}
