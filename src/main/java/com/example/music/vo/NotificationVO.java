package com.example.music.vo;

import com.example.music.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 通知展示 VO
 * <p>
 * 从 Notification 实体转换，供前端展示通知列表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationVO {

    private Long id;
    private String type;
    private String title;
    private String content;
    private Boolean isRead;
    private String relatedType;
    private Long relatedId;
    private LocalDateTime createdTime;

    /** 格式化后的时间描述（如 "3 分钟前"），由前端计算 */
    private String timeAgo;

    /**
     * 从 Notification 实体构建 VO
     */
    public static NotificationVO fromEntity(Notification notification) {
        if (notification == null) return null;
        return NotificationVO.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .content(notification.getContent())
                .isRead(notification.getIsRead())
                .relatedType(notification.getRelatedType())
                .relatedId(notification.getRelatedId())
                .createdTime(notification.getCreatedTime())
                .build();
    }
}