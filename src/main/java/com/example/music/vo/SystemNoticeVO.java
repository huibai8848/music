package com.example.music.vo;

import com.example.music.entity.SystemNotice;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统公告展示 VO
 */
@Data
public class SystemNoticeVO {

    private Long id;
    private String title;
    private String content;
    private String type;
    private Boolean isActive;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    /**
     * 从 SystemNotice 实体转换
     */
    public static SystemNoticeVO fromEntity(SystemNotice notice) {
        if (notice == null) return null;
        SystemNoticeVO vo = new SystemNoticeVO();
        vo.setId(notice.getId());
        vo.setTitle(notice.getTitle());
        vo.setContent(notice.getContent());
        vo.setType(notice.getType());
        vo.setIsActive(notice.getIsActive());
        vo.setCreatedTime(notice.getCreatedTime());
        vo.setUpdatedTime(notice.getUpdatedTime());
        return vo;
    }
}
