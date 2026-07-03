package com.example.music.service;

import com.example.music.vo.ReportVO;

import java.util.Map;

/**
 * 举报服务接口
 * <p>
 * 支持提交举报、查询我的举报记录。
 * 管理端的举报处理接口在 admin 模块实现。
 */
public interface ReportService {

    /**
     * 提交举报
     * <p>
     * 校验：同一用户 24h 内不可重复举报同一内容。
     *
     * @param reporterId  举报人 ID
     * @param targetType  目标类型
     * @param targetId    目标 ID
     * @param reason      举报原因
     * @param description 补充说明
     */
    void submitReport(Long reporterId, String targetType, Long targetId,
                      String reason, String description);

    /**
     * 获取我的举报记录（分页，按时间降序）
     */
    Map<String, Object> getMyReports(Long reporterId, int page, int size);
}