package com.example.music.service;

import java.util.Map;

/**
 * 管理后台操作日志服务接口
 * <p>
 * 提供管理员操作日志的分页查询功能。
 */
public interface AdminLogService {

    /**
     * 分页查询操作日志
     *
     * @param action     筛选操作类型（可选）
     * @param operatorId 筛选操作人（可选）
     * @param page       页码
     * @param size       每页条数
     * @return 分页结果（records + total + page + size）
     */
    Map<String, Object> listLogs(String action, Long operatorId, int page, int size);
}
