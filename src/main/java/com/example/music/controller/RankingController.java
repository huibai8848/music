package com.example.music.controller;

import com.example.music.constant.RedisKeys;
import com.example.music.service.SongService;
import com.example.music.utils.CacheUtil;
import com.example.music.vo.R;
import com.example.music.vo.SongVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 排行榜控制器
 * <p>
 * 基于 Redis ZSet 实现日榜/周榜/月榜/总榜。
 * 播放量上报时同步更新 ZSet 分数（原子自增）。
 */
@Slf4j
@RestController
@RequestMapping("/api/rankings")
@RequiredArgsConstructor
public class RankingController {

    private final SongService songService;
    private final CacheUtil cacheUtil;

    /**
     * 获取排行榜
     *
     * @param type  榜单类型：daily / weekly / monthly / all
     * @param limit 返回条数（默认 50）
     */
    @GetMapping
    public R<Map<String, Object>> getRankings(
            @RequestParam(defaultValue = "daily") String type,
            @RequestParam(defaultValue = "50") int limit) {

        String redisKey;
        String rangeLabel;

        switch (type.toLowerCase()) {
            case "daily":
                String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                redisKey = RedisKeys.RANKING_DAILY + today;
                rangeLabel = "日榜";
                break;
            case "weekly":
                String week = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy'w'ww"));
                redisKey = RedisKeys.RANKING_WEEKLY + week;
                rangeLabel = "周榜";
                break;
            case "monthly":
                String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
                redisKey = RedisKeys.RANKING_MONTHLY + month;
                rangeLabel = "月榜";
                break;
            case "all":
                redisKey = RedisKeys.RANKING_ALL;
                rangeLabel = "总榜";
                break;
            default:
                return R.fail(400, "无效的榜单类型: " + type);
        }

        // 从 ZSet 获取前 N 个（分数从高到低）
        Set<Object> topIds = cacheUtil.zReverseRange(redisKey, 0, limit - 1);
        if (topIds == null || topIds.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("type", type);
            result.put("label", rangeLabel);
            result.put("songs", Collections.emptyList());
            result.put("total", 0);
            return R.ok(result);
        }

        // 获取歌曲详情并排序
        List<SongVO> songs = new ArrayList<>();
        for (Object idObj : topIds) {
            try {
                Long songId = Long.parseLong(idObj.toString());
                SongVO song = songService.getSongDetail(songId);
                if (song != null) songs.add(song);
            } catch (Exception e) {
                log.warn("排行榜获取歌曲详情失败: songId={}", idObj, e);
            }
        }

        // 按 ZSet 顺序保持排名
        Map<Long, Integer> rankMap = new LinkedHashMap<>();
        int rank = 1;
        for (Object idObj : topIds) {
            rankMap.put(Long.parseLong(idObj.toString()), rank++);
        }
        songs.sort(Comparator.comparingInt(a ->
                rankMap.getOrDefault(a.getId(), 999)));

        Map<String, Object> result = new HashMap<>();
        result.put("type", type);
        result.put("label", rangeLabel);
        result.put("songs", songs);
        result.put("total", songs.size());

        return R.ok(result);
    }
}
