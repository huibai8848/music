package com.example.music.service;

import com.example.music.vo.DashboardVO;

/**
 * 管理后台数据看板服务接口
 * <p>
 * 提供平台核心数据的统计汇总，供管理员首页看板展示。
 */
public interface AdminDashboardService {

    /**
     * 获取数据看板概览
     * <p>
     * 包含用户数、歌曲数、专辑数、艺人数、待处理数据等关键指标。
     *
     * @return 看板数据
     */
    DashboardVO getDashboard();
}
