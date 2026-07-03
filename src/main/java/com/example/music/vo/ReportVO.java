package com.example.music.vo;

import com.example.music.entity.Report;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 举报展示 VO
 * <p>
 * 展示举报记录，包含举报人信息和处理状态。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportVO {

    private Long id;
    private String targetType;
    private Long targetId;
    private String reason;
    private String description;
    private String status;
    private String handleNote;
    private LocalDateTime createdTime;
    private LocalDateTime handledTime;

    /** 举报人昵称（管理端展示用） */
    private String reporterNickname;

    /** 举报人头像 */
    private String reporterAvatar;

    /**
     * 从 Report 实体构建 VO（用户端：不暴露举报人信息）
     */
    public static ReportVO fromEntity(Report report) {
        if (report == null) return null;
        return ReportVO.builder()
                .id(report.getId())
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .reason(report.getReason())
                .description(report.getDescription())
                .status(report.getStatus())
                .handleNote(report.getHandleNote())
                .createdTime(report.getCreatedTime())
                .handledTime(report.getHandledTime())
                .build();
    }

    /**
     * 从 Report 实体构建含举报人信息的 VO（管理端使用）
     */
    public static ReportVO fromEntityWithReporter(Report report, String nickname, String avatar) {
        ReportVO vo = fromEntity(report);
        if (vo != null) {
            vo.setReporterNickname(nickname);
            vo.setReporterAvatar(avatar);
        }
        return vo;
    }
}