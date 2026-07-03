package com.example.music.service.impl;

import com.example.music.mapper.*;
import com.example.music.service.AdminDashboardService;
import com.example.music.vo.DashboardVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据看板服务实现
 * <p>
 * 聚合各 Mapper 的统计方法，组合成看板数据。
 * 所有计数均为实时查询，不缓存。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final UserMapper userMapper;
    private final SongMapper songMapper;
    private final AlbumMapper albumMapper;
    private final ArtistMapper artistMapper;
    private final PlaylistMapper playlistMapper;
    private final CommentMapper commentMapper;
    private final ReportMapper reportMapper;
    private final PlayHistoryMapper playHistoryMapper;

    @Override
    public DashboardVO getDashboard() {
        long totalUsers = userMapper.countTotal();
        long totalVipUsers = userMapper.countVip();
        long totalSongs = songMapper.countTotal(null, null, null, null, null, null);
        long totalAlbums = albumMapper.countTotal();
        long totalArtists = artistMapper.countTotal();
        long totalPlaylists = playlistMapper.countTotal();
        long totalComments = commentMapper.countAllForAdmin(null);
        long todayNewUsers = userMapper.countToday();
        long pendingReports = reportMapper.countByStatus("PENDING");
        long pendingSongs = songMapper.countTotal(null, null, null, null, null, "PENDING");
        long totalPlayCount = songMapper.sumPlayCount();

        log.debug("管理员查看看板数据: {} 用户, {} 歌曲, {} 播放量",
                totalUsers, totalSongs, totalPlayCount);

        // 构建近 7 天趋势数据
        List<String> labels = new ArrayList<>();
        List<Long> newUsers = new ArrayList<>();
        List<Long> playCounts = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/dd");

        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            labels.add(date.format(fmt));
            // 每日用户数据通过 countTodayByDate 查询
            long dailyUsers = userMapper.countByDate(date);
            newUsers.add(dailyUsers);
            // 每日播放量（从 play_history 表按日聚合）
            long dailyPlay = playHistoryMapper.countByDate(date);
            playCounts.add(dailyPlay);
        }

        // 风格分布（从歌曲表统计）
        List<DashboardVO.GenreCount> genreDist = new ArrayList<>();
        try {
            List<Map<String, Object>> genreStats = songMapper.countByGenre();
            for (Map<String, Object> row : genreStats) {
                String name = (String) row.get("genre");
                Object countObj = row.get("count");
                if (name != null && countObj != null) {
                    genreDist.add(new DashboardVO.GenreCount(
                            name, ((Number) countObj).longValue()));
                }
            }
        } catch (Exception e) {
            log.warn("获取风格分布异常", e);
        }

        return new DashboardVO(
                totalUsers, totalVipUsers, totalSongs, totalAlbums, totalArtists,
                totalPlaylists, totalComments, todayNewUsers,
                pendingReports, pendingSongs, totalPlayCount,
                labels, newUsers, playCounts, genreDist
        );
    }
}
