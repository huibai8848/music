package com.example.music.controller;

import com.example.music.aspect.OperationLog;
import com.example.music.aspect.RateLimit;
import com.example.music.entity.Song;
import com.example.music.service.SongService;
import com.example.music.vo.R;
import com.example.music.vo.SongVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 歌曲控制器
 * <p>
 * 提供歌曲列表、详情、搜索、播放量上报、热门歌曲等接口。
 * 部分接口无需登录即可访问（白名单已配置）。
 */
@RestController
@RequestMapping("/api/songs")
@RequiredArgsConstructor
public class SongController {

    private final SongService songService;

    /**
     * 歌曲列表（分页，可选按风格/状态筛选）
     *
     * @param page  页码（默认 1）
     * @param size  每页条数（默认 20）
     * @param genre 风格筛选（可选）
     */
    @GetMapping
    public R<Map<String, Object>> listSongs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String genre) {

        List<SongVO> songs = songService.listSongs(page, size, genre, "ACTIVE");
        long total = songService.countSongs(genre, "ACTIVE");

        Map<String, Object> result = new HashMap<>();
        result.put("records", songs);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("pages", (int) Math.ceil((double) total / size));

        return R.ok(result);
    }

    /**
     * 模糊搜索（同时搜索歌曲名/艺人名/专辑名）
     *
     * @param q    搜索关键词
     * @param page 页码
     * @param size 每页条数
     */
    @GetMapping("/search")
    public R<Map<String, Object>> searchSongs(
            @RequestParam String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<SongVO> songs = songService.searchSongs(q, page, size);
        long total = songService.countSearch(q);

        Map<String, Object> result = new HashMap<>();
        result.put("records", songs);
        result.put("total", total);
        result.put("keyword", q);
        return R.ok(result);
    }

    /**
     * 歌曲详情
     */
    @GetMapping("/{id}")
    public R<SongVO> getSongDetail(@PathVariable Long id) {
        return R.ok(songService.getSongDetail(id));
    }

    /**
     * 获取 LRC 歌词 JSON
     */
    @GetMapping("/{id}/lyrics")
    public R<String> getLyrics(@PathVariable Long id) {
        return R.ok("获取成功", songService.getLyrics(id));
    }

    /**
     * 播放计数上报
     * 限流：每用户每分钟最多 60 次（避免恶意刷量）
     */
    @PostMapping("/{id}/play")
    @RateLimit(count = 60, time = 60)
    public R<Object> reportPlay(@PathVariable Long id) {
        songService.reportPlay(id);
        return R.ok();
    }

    /**
     * 热门歌曲
     */
    @GetMapping("/hot")
    public R<List<SongVO>> getHotSongs(
            @RequestParam(defaultValue = "20") int limit) {
        return R.ok(songService.getHotSongs(limit));
    }

    // ==================== 管理接口（以下需要 ADMIN 角色） ====================

    /**
     * 新增歌曲（管理员）
     */
    @PostMapping
    @OperationLog(value = "新增歌曲", targetType = "SONG")
    public R<SongVO> createSong(@RequestBody Song song) {
        return R.ok("创建成功", songService.createSong(song));
    }

    /**
     * 更新歌曲（管理员）
     */
    @PutMapping
    @OperationLog(value = "更新歌曲", targetType = "SONG")
    public R<SongVO> updateSong(@RequestBody Song song) {
        return R.ok("更新成功", songService.updateSong(song));
    }

    /**
     * 删除歌曲（管理员）
     */
    @DeleteMapping("/{id}")
    @OperationLog(value = "删除歌曲", targetType = "SONG")
    public R<Object> deleteSong(@PathVariable Long id) {
        songService.deleteSong(id);
        return R.ok("删除成功");
    }

    /**
     * 获取歌曲分类 ID 列表
     */
    @GetMapping("/{id}/categories")
    public R<List<Long>> getSongCategories(@PathVariable Long id) {
        return R.ok(songService.getSongCategoryIds(id));
    }

    /**
     * 更新歌曲分类
     */
    @PutMapping("/{id}/categories")
    public R<Object> updateSongCategories(@PathVariable Long id, @RequestBody List<Long> categoryIds) {
        songService.updateSongCategories(id, categoryIds);
        return R.ok("分类更新成功");
    }
}