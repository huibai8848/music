package com.example.music.service;

import com.example.music.vo.NotificationVO;

import java.util.Map;

/**
 * 通知服务接口
 * <p>
 * 支持通知列表查询、标记已读、未读计数，以及通知创建（供其他模块调用）。
 */
public interface NotificationService {

    /**
     * 获取当前用户的通知列表（分页，按时间降序）
     */
    Map<String, Object> getNotifications(Long userId, int page, int size);

    /**
     * 标记单条通知为已读
     */
    void markRead(Long notificationId, Long userId);

    /**
     * 标记当前用户所有通知为已读
     */
    void markAllRead(Long userId);

    /**
     * 获取当前用户未读通知数
     */
    long getUnreadCount(Long userId);

    /**
     * 创建通知（供其他模块调用）
     *
     * @param userId      接收者 ID
     * @param type        通知类型（REPLY / SYSTEM / MEMBERSHIP / REPORT_RESULT）
     * @param title       通知标题
     * @param content     通知内容
     * @param relatedType 关联类型
     * @param relatedId   关联 ID
     */
    void createNotification(Long userId, String type, String title,
                            String content, String relatedType, Long relatedId);
}