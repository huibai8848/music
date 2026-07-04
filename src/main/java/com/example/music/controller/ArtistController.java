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
 * 艺人列表、搜索和详情，均支持分页。
 * 所有公开查询仅返回有 ACTIVE 歌曲的艺人，审核中/已驳回的歌曲不计数。
 */
@RestController
@RequestMapping("/api/artists")
@RequiredArgsConstructor
public class ArtistController {

    private final ArtistService artistService;

    /**
     * 艺人列表（分页）
     * <p>
     * 仅返回拥有 ACTIVE（审核通过）歌曲的艺人，
     * 列表中每个艺人携带其 ACTIVE 歌曲数量（songCount）。
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
     * 模糊搜索艺人（分页）
     * <p>
     * 根据关键词模糊匹配艺人名和歌曲名，仅返回有 ACTIVE 歌曲的艺人。
     * 按相关性排序：艺人名匹配 > 歌曲名匹配。
     *
     * @param q    搜索关键词（模糊匹配艺人名 + 歌曲名）
     * @param page 页码（默认 1）
     * @param size 每页条数（默认 20）
     */
    @GetMapping("/search")
    public R<Map<String, Object>> searchArtists(
            @RequestParam String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<ArtistVO> artists = artistService.searchArtists(q, page, size);
        long total = artistService.countSearch(q);

        Map<String, Object> result = new HashMap<>();
        result.put("records", artists);
        result.put("total", total);
        result.put("keyword", q);
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