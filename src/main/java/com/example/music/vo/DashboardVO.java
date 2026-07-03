package com.example.music.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 管理后台数据看板 VO
 * <p>
 * 包含平台核心统计数据，供管理后台首页看板展示。
 * 新增图表趋势数据字段，用于 ECharts/Chart.js 展示。
 */
@Data
@AllArgsConstructor
public class DashboardVO {

    /** 用户总数 */
    private long totalUsers;

    /** 会员总数 */
    private long totalVipUsers;

    /** 歌曲总数 */
    private long totalSongs;

    /** 专辑总数 */
    private long totalAlbums;

    /** 艺人总数 */
    private long totalArtists;

    /** 歌单总数 */
    private long totalPlaylists;

    /** 评论总数 */
    private long totalComments;

    /** 今日新增用户 */
    private long todayNewUsers;

    /** 待处理举报数 */
    private long pendingReports;

    /** 待审核歌曲数 */
    private long pendingSongs;

    /** 总播放量 */
    private long totalPlayCount;

    // ==================== 图表趋势数据 ====================

    /** 近 7 天日期标签 */
    private List<String> last7DaysLabels;

    /** 近 7 天每日新增用户数 */
    private List<Long> last7DaysNewUsers;

    /** 近 7 天每日播放量 */
    private List<Long> last7DaysPlayCount;

    /** 各风格歌曲数量分布（饼图） */
    private List<GenreCount> genreDistribution;

    /** 风格计数内部类 */
    @Data
    @AllArgsConstructor
    public static class GenreCount {
        private String name;
        private long count;
    }
}