package com.example.music.controller;

import com.example.music.service.SongService;
import com.example.music.vo.R;
import com.example.music.vo.SongVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 排行榜控制器
 * <p>
 * 所有榜单（日榜/周榜/月榜/总榜）均从数据库查询，
 * 按 song.play_count DESC 排序，保证数据准确性。
 */
@Slf4j
@RestController
@RequestMapping("/api/rankings")
@RequiredArgsConstructor
public class RankingController {

    private final SongService songService;

    /**
     * 获取排行榜
     * <p>
     * 所有榜单均从数据库按总播放量降序查询，
     * 播放量由 PlayHistoryService 在每次播放时同步更新。
     *
     * @param type  榜单类型：daily / weekly / monthly / all
     * @param limit 返回条数（默认 50）
     */
    @GetMapping
    public R<Map<String, Object>> getRankings(
            @RequestParam(defaultValue = "daily") String type,
            @RequestParam(defaultValue = "50") int limit) {

        String rangeLabel;

        switch (type.toLowerCase()) {
            case "daily":
                rangeLabel = "日榜";
                break;
            case "weekly":
                rangeLabel = "周榜";
                break;
            case "monthly":
                rangeLabel = "月榜";
                break;
            case "all":
                rangeLabel = "总榜";
                break;
            default:
                return R.fail(400, "无效的榜单类型: " + type);
        }

        // 所有榜单统一从数据库按 play_count DESC 排序
        List<SongVO> songs = songService.getHotSongs(limit);

        Map<String, Object> result = new HashMap<>();
        result.put("type", type);
        result.put("label", rangeLabel);
        result.put("songs", songs);
        result.put("total", songs.size());

        return R.ok(result);
    }
}
