package com.example.music.controller;

import com.example.music.service.AlbumService;
import com.example.music.vo.AlbumVO;
import com.example.music.vo.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 专辑控制器
 * <p>
 * 专辑列表、搜索和详情，支持分页。
 * 所有公开查询仅返回有 ACTIVE 歌曲的专辑。
 */
@RestController
@RequestMapping("/api/albums")
@RequiredArgsConstructor
public class AlbumController {

    private final AlbumService albumService;

    /**
     * 专辑列表（分页）
     * <p>
     * 仅返回有 ACTIVE（审核通过）歌曲的专辑。
     * 列表项不含歌曲详情，仅展示专辑基本信息 + 艺人名。
     */
    @GetMapping
    public R<Map<String, Object>> listAlbums(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<AlbumVO> albums = albumService.listAlbums(page, size);
        long total = albumService.countAlbums();

        Map<String, Object> result = new HashMap<>();
        result.put("records", albums);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return R.ok(result);
    }

    /**
     * 模糊搜索专辑（分页）
     * <p>
     * 根据关键词模糊匹配专辑名和艺人名，仅返回有 ACTIVE 歌曲的专辑。
     * 按相关性排序：专辑名匹配 > 艺人名匹配。
     *
     * @param q    搜索关键词
     * @param page 页码（默认 1）
     * @param size 每页条数（默认 20）
     */
    @GetMapping("/search")
    public R<Map<String, Object>> searchAlbums(
            @RequestParam String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<AlbumVO> albums = albumService.searchAlbums(q, page, size);
        long total = albumService.countSearchAlbums(q);

        Map<String, Object> result = new HashMap<>();
        result.put("records", albums);
        result.put("total", total);
        result.put("keyword", q);
        result.put("page", page);
        result.put("size", size);
        return R.ok(result);
    }

    /**
     * 专辑详情（含歌曲列表）
     * <p>
     * 返回专辑基本信息 + 所属艺人名 + 专辑内歌曲列表（含所有状态）。
     * PENDING 歌曲在前端通过 enrichSongVO 的 ACTIVE 判断控制显示。
     */
    @GetMapping("/{id}")
    public R<AlbumVO> getAlbumDetail(@PathVariable Long id) {
        return R.ok(albumService.getAlbumDetail(id));
    }
}
