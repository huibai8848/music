package com.example.music.service;

import java.util.Map;

/**
 * 管理后台用户管理服务接口
 * <p>
 * 提供用户列表查询、搜索、封禁/解封等功能。
 */
public interface AdminUserService {

    /**
     * 分页查询用户列表（支持按昵称/邮箱搜索）
     *
     * @param keyword 搜索关键字（可选）
     * @param page    页码
     * @param size    每页条数
     * @return 分页结果（records + total + page + size）
     */
    Map<String, Object> listUsers(String keyword, int page, int size);

    /**
     * 封禁/解封用户
     *
     * @param adminId 操作管理员 ID
     * @param userId  目标用户 ID
     * @param banned  true=封禁，false=解封
     */
    void banUser(Long adminId, Long userId, boolean banned);

    /**
     * 统计所有用户数（含可选关键字搜索）
     *
     * @param keyword 搜索关键字（可选）
     * @return 用户总数
     */
    long countAll(String keyword);
}
