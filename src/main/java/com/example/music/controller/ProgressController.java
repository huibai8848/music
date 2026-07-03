package com.example.music.controller;

import com.example.music.constant.RedisKeys;
import com.example.music.utils.CacheUtil;
import com.example.music.utils.RequestContext;
import com.example.music.vo.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 播放进度控制器
 * <p>
 * 记录用户每首歌的播放进度（毫秒级），用于"续播"功能。
 * 数据存储在 Redis 中（TTL 7 天），不入 MySQL。
 * <p>
 * 前端每 10 秒上报一次进度，进入歌曲时查询是否有历史进度。
 */
@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final CacheUtil cacheUtil;

    /**
     * 上报播放进度
     * <p>
     * 前端每 10 秒调用一次，将当前播放进度存入 Redis。
     *
     * @param body JSON body: {"songId": 123, "progress": 45000}
     */
    @PostMapping
    public R<Object> reportProgress(@RequestBody Map<String, Object> body) {
        Long userId = RequestContext.getUserId();
        if (userId == null) {
            return R.fail("请先登录");
        }

        Long songId = body.get("songId") != null ? ((Number) body.get("songId")).longValue() : null;
        Long progress = body.get("progress") != null ? ((Number) body.get("progress")).longValue() : null;
        if (songId == null || progress == null) {
            return R.fail("缺少 songId 或 progress");
        }

        String key = RedisKeys.PLAY_PROGRESS + userId + ":" + songId;
        cacheUtil.set(key, String.valueOf(progress), RedisKeys.TTL_PROGRESS, TimeUnit.SECONDS);
        return R.ok();
    }

    /**
     * 获取播放进度
     * <p>
     * 进入歌曲时调用，返回上次保存的播放进度（毫秒）。
     * 前端如有值则弹窗提示"检测到上次听到 xx:xx，是否续播？"
     *
     * @param songId 歌曲 ID
     * @return 播放进度（毫秒），无记录返回 0
     */
    @GetMapping("/{songId}")
    public R<Long> getProgress(@PathVariable Long songId) {
        Long userId = RequestContext.getUserId();
        if (userId == null) {
            return R.ok(0L);
        }

        String key = RedisKeys.PLAY_PROGRESS + userId + ":" + songId;
        String value = cacheUtil.get(key, String.class);

        if (value == null) {
            return R.ok(0L);
        }

        try {
            return R.ok(Long.parseLong(value));
        } catch (NumberFormatException e) {
            return R.ok(0L);
        }
    }
}
