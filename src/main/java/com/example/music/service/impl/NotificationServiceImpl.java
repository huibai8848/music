package com.example.music.service.impl;

import com.example.music.config.RedisMessagePublisher;
import com.example.music.constant.ErrorCode;
import com.example.music.constant.RedisChannels;
import com.example.music.entity.Notification;
import com.example.music.exception.BusinessException;
import com.example.music.mapper.NotificationMapper;
import com.example.music.service.NotificationService;
import com.example.music.vo.NotificationVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 通知服务实现
 * <p>
 * 核心逻辑：
 * 1. 通知列表支持分页，按时间降序返回
 * 2. 标记已读校验通知所有权
 * 3. 提供 createNotification 方法供其他模块（评论/举报/定时任务）调用
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final RedisMessagePublisher redisPublisher;
    private final ObjectMapper objectMapper;

    @Override
    public Map<String, Object> getNotifications(Long userId, int page, int size) {
        int offset = (page - 1) * size;

        List<Notification> list = notificationMapper.selectByUserId(userId, offset, size);
        long total = notificationMapper.countByUserId(userId);

        List<NotificationVO> voList = list.stream()
                .map(NotificationVO::fromEntity)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("records", voList);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long notificationId, Long userId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null) {
            throw new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        // 只能标记自己的通知
        if (!notification.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        notificationMapper.markRead(notificationId, userId);
        log.debug("标记通知已读: id={}, userId={}", notificationId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllRead(Long userId) {
        notificationMapper.markAllRead(userId);
        log.debug("全部标记已读: userId={}", userId);
    }

    @Override
    public long getUnreadCount(Long userId) {
        return notificationMapper.countUnread(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createNotification(Long userId, String type, String title,
                                   String content, String relatedType, Long relatedId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setIsRead(false);
        notification.setRelatedType(relatedType);
        notification.setRelatedId(relatedId);

        notificationMapper.insert(notification);
        log.info("创建通知: userId={}, type={}, title={}", userId, type, title);

        // Redis Pub/Sub 推送通知到用户 WebSocket —— 延迟到事务提交后执行
        Long notificationId = notification.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    Map<String, Object> notifyMsg = new HashMap<>();
                    notifyMsg.put("userId", userId);
                    notifyMsg.put("id", notificationId);
                    notifyMsg.put("type", type);
                    notifyMsg.put("title", title);
                    notifyMsg.put("content", content);
                    notifyMsg.put("relatedType", relatedType);
                    notifyMsg.put("relatedId", relatedId);
                    notifyMsg.put("isRead", false);
                    notifyMsg.put("createdTime", System.currentTimeMillis());

                    String json = objectMapper.writeValueAsString(notifyMsg);
                    redisPublisher.publish(RedisChannels.USER_NOTIFY, json);
                } catch (JsonProcessingException e) {
                    log.error("通知序列化失败，userId={}, type={}", userId, type, e);
                }
            }
        });
    }
}
