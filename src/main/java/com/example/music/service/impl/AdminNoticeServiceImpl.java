package com.example.music.service.impl;

import com.example.music.config.RedisMessagePublisher;
import com.example.music.constant.ErrorCode;
import com.example.music.constant.RedisChannels;
import com.example.music.dto.NoticeDTO;
import com.example.music.entity.Notification;
import com.example.music.entity.SystemNotice;
import com.example.music.entity.User;
import com.example.music.exception.BusinessException;
import com.example.music.mapper.NotificationMapper;
import com.example.music.mapper.SystemNoticeMapper;
import com.example.music.mapper.UserMapper;
import com.example.music.service.AdminNoticeService;
import com.example.music.service.NotificationService;
import com.example.music.vo.SystemNoticeVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统公告管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminNoticeServiceImpl implements AdminNoticeService {

    private final SystemNoticeMapper noticeMapper;
    private final RedisMessagePublisher redisPublisher;
    private final ObjectMapper objectMapper;
    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    @Override
    public Map<String, Object> listNotices(String type, int page, int size) {
        int offset = (page - 1) * size;

        List<SystemNotice> list = noticeMapper.selectList(offset, size, type);
        long total = noticeMapper.countList(type);

        List<SystemNoticeVO> voList = list.stream()
                .map(SystemNoticeVO::fromEntity)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("records", voList);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    @Override
    public SystemNoticeVO getNotice(Long id) {
        SystemNotice notice = noticeMapper.selectById(id);
        return SystemNoticeVO.fromEntity(notice);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SystemNoticeVO createNotice(NoticeDTO dto) {
        SystemNotice notice = new SystemNotice();
        notice.setTitle(dto.getTitle());
        notice.setContent(dto.getContent());
        notice.setType(dto.getType());
        notice.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);

        noticeMapper.insert(notice);
        log.info("新增系统公告: id={}, title={}", notice.getId(), notice.getTitle());

        // Redis Pub/Sub 广播系统公告到所有在线用户 —— 延迟到事务提交后执行
        SystemNotice finalNotice = notice;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    Map<String, Object> broadcastMsg = new HashMap<>();
                    broadcastMsg.put("type", "SYSTEM_BROADCAST");
                    broadcastMsg.put("id", finalNotice.getId());
                    broadcastMsg.put("title", finalNotice.getTitle());
                    broadcastMsg.put("content", finalNotice.getContent());
                    broadcastMsg.put("createdTime", finalNotice.getCreatedTime());

                    String json = objectMapper.writeValueAsString(broadcastMsg);
                    redisPublisher.publish(RedisChannels.SYSTEM_BROADCAST, json);
                } catch (JsonProcessingException e) {
                    log.error("系统公告序列化失败，id={}", finalNotice.getId(), e);
                }

                // 向所有 USER 和 VIP 用户发送数据库通知
                notifyAllUsers(finalNotice);
            }
        });

        return SystemNoticeVO.fromEntity(notice);
    }

    /**
     * 系统公告发布后，向所有普通用户和 VIP 用户发送通知
     */
    private void notifyAllUsers(SystemNotice notice) {
        try {
            List<User> users = userMapper.selectByRoles(Arrays.asList("USER", "VIP"));
            if (users == null || users.isEmpty()) {
                log.debug("没有需要通知的普通/VIP用户");
                return;
            }

            List<Notification> notifications = users.stream().map(user -> {
                Notification n = new Notification();
                n.setUserId(user.getId());
                n.setType("SYSTEM");
                n.setTitle(notice.getTitle());
                n.setContent(notice.getContent());
                n.setIsRead(false);
                n.setRelatedType("NOTICE");
                n.setRelatedId(notice.getId());
                return n;
            }).collect(Collectors.toList());

            // 分批插入（每批 500 条）
            int batchSize = 500;
            for (int i = 0; i < notifications.size(); i += batchSize) {
                int end = Math.min(i + batchSize, notifications.size());
                notificationMapper.insertBatch(notifications.subList(i, end));
            }
            log.info("系统公告已通知 {} 位用户: id={}, title={}", users.size(), notice.getId(), notice.getTitle());
        } catch (Exception e) {
            log.error("发送系统公告通知失败: id={}, title={}", notice.getId(), notice.getTitle(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SystemNoticeVO updateNotice(Long id, NoticeDTO dto) {
        SystemNotice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BusinessException(ErrorCode.NOTICE_NOT_FOUND);
        }

        if (dto.getTitle() != null) notice.setTitle(dto.getTitle());
        if (dto.getContent() != null) notice.setContent(dto.getContent());
        if (dto.getType() != null) notice.setType(dto.getType());
        if (dto.getIsActive() != null) notice.setIsActive(dto.getIsActive());

        noticeMapper.update(notice);
        log.info("更新系统公告: id={}, title={}", id, notice.getTitle());
        return SystemNoticeVO.fromEntity(notice);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNotice(Long id) {
        noticeMapper.deleteById(id);
        log.info("删除系统公告: id={}", id);
    }

    @Override
    public List<SystemNoticeVO> getActiveNotices() {
        return noticeMapper.selectActive().stream()
                .map(SystemNoticeVO::fromEntity)
                .collect(Collectors.toList());
    }
}
