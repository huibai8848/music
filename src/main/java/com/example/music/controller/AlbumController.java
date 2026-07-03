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
 * 专辑列表和详情，支持分页。
 */
@RestController
@RequestMapping("/api/albums")
@RequiredArgsConstructor
public class AlbumController {

    private final AlbumService albumService;

    /**
     * 专辑列表（分页）
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
     * 专辑详情（含歌曲列表）
     */
    @GetMapping("/{id}")
    public R<AlbumVO> getAlbumDetail(@PathVariable Long id) {
        return R.ok(albumService.getAlbumDetail(id));
    }
}