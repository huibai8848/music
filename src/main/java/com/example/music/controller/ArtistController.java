package com.example.music.controller;

import com.example.music.service.ArtistService;
import com.example.music.vo.ArtistVO;
import com.example.music.vo.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 艺人控制器
 * <p>
 * 艺人列表和详情，支持分页。
 */
@RestController
@RequestMapping("/api/artists")
@RequiredArgsConstructor
public class ArtistController {

    private final ArtistService artistService;

    /**
     * 艺人列表（分页）
     */
    @GetMapping
    public R<Map<String, Object>> listArtists(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<ArtistVO> artists = artistService.listArtists(page, size);
        long total = artistService.countArtists();

        Map<String, Object> result = new HashMap<>();
        result.put("records", artists);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return R.ok(result);
    }

    /**
     * 艺人详情（含作品列表和专辑列表）
     */
    @GetMapping("/{id}")
    public R<ArtistVO> getArtistDetail(@PathVariable Long id) {
        return R.ok(artistService.getArtistDetail(id));
    }
}