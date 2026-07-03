package com.example.music.service;

import java.util.Map;

/**
 * 管理后台举报管理服务接口
 * <p>
 * 提供举报列表查看和处理功能。
 * 处理后通过通知模块通知举报人处理结果。
 */
public interface AdminReportService {

    /**
     * 分页查询举报列表（管理端）
     *
     * @param status 筛选状态（PENDING / RESOLVED / DISMISSED，可选为 null）
     * @param page   页码
     * @param size   每页条数
     * @return 分页结果（含举报人信息）
     */
    Map<String, Object> listReports(String status, int page, int size);

    /**
     * 处理举报
     *
     * @param adminId   操作管理员 ID
     * @param reportId  举报 ID
     * @param status    处理结果（RESOLVED=确认违规 / DISMISSED=驳回）
     * @param handleNote 处理备注
     */
    void handleReport(Long adminId, Long reportId, String status, String handleNote);
}
