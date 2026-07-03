package com.example.music.controller;

import com.example.music.service.NotificationService;
import com.example.music.utils.RequestContext;
import com.example.music.vo.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 通知控制器
 * <p>
 * 提供通知列表查询、标记已读、全部已读、未读计数等接口。
 * 通知的创建由其他模块（评论回复、系统公告、举报结果等）内部调用。
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 获取当前用户的通知列表（分页，按时间降序）
     *
     * @param page 页码，从 1 开始
     * @param size 每页条数，默认 20
     */
    @GetMapping
    public R<Map<String, Object>> getNotifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = RequestContext.getUserId();
        if (userId == null) {
            return R.fail("请先登录");
        }
        return R.ok(notificationService.getNotifications(userId, page, size));
    }

    /**
     * 标记单条通知为已读
     *
     * @param id 通知 ID
     */
    @PutMapping("/{id}/read")
    public R<Object> markRead(@PathVariable Long id) {
        Long userId = RequestContext.getUserId();
        if (userId == null) {
            return R.fail("请先登录");
        }
        notificationService.markRead(id, userId);
        return R.ok();
    }

    /**
     * 标记当前用户所有通知为已读
     */
    @PutMapping("/read-all")
    public R<Object> markAllRead() {
        Long userId = RequestContext.getUserId();
        if (userId == null) {
            return R.fail("请先登录");
        }
        notificationService.markAllRead(userId);
        return R.ok();
    }

    /**
     * 获取当前用户未读通知数量
     * <p>
     * 前端可用此接口轮询（或 WebSocket 推送）更新角标。
     */
    @GetMapping("/unread-count")
    public R<Long> getUnreadCount() {
        Long userId = RequestContext.getUserId();
        if (userId == null) {
            return R.ok(0L);
        }
        return R.ok(notificationService.getUnreadCount(userId));
    }
}