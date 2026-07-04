package com.example.music.service.impl;

import com.example.music.constant.ErrorCode;
import com.example.music.entity.Comment;
import com.example.music.entity.Report;
import com.example.music.entity.User;
import com.example.music.exception.BusinessException;
import com.example.music.mapper.CommentMapper;
import com.example.music.mapper.ReportMapper;
import com.example.music.mapper.UserMapper;
import com.example.music.service.AdminReportService;
import com.example.music.service.NotificationService;
import com.example.music.vo.ReportVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理后台举报管理服务实现
 * <p>
 * 核心逻辑：
 * 1. 举报列表按状态筛选，展示举报人和被举报内容信息
 * 2. 处理举报后通过 NotificationService 通知举报人
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminReportServiceImpl implements AdminReportService {

    private final ReportMapper reportMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final CommentMapper commentMapper;

    @Override
    public Map<String, Object> listReports(String status, int page, int size) {
        int offset = (page - 1) * size;

        String queryStatus = (status != null && !status.isEmpty()) ? status : null;
        List<Report> list = reportMapper.selectByStatus(queryStatus, offset, size);
        long total = reportMapper.countByStatus(queryStatus);

        // 为每条举报填充举报人信息
        List<ReportVO> voList = list.stream().map(report -> {
            User reporter = userMapper.selectById(report.getReporterId());
            String nickname = reporter != null ? reporter.getNickname() : "未知用户";
            String avatar = reporter != null ? reporter.getAvatar() : null;
            return ReportVO.fromEntityWithReporter(report, nickname, avatar);
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("records", voList);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleReport(Long adminId, Long reportId, String status, String handleNote) {
        // 校验举报存在
        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException(ErrorCode.ADMIN_REPORT_NOT_FOUND);
        }

        // 更新举报状态（乐观锁：仅 PENDING 状态可被处理，防止并发重复处理）
        int affected = reportMapper.handle(reportId, status, adminId, handleNote, LocalDateTime.now());
        if (affected == 0) {
            throw new BusinessException(ErrorCode.ADMIN_REPORT_ALREADY_HANDLED);
        }
        log.info("管理员 {} 处理举报 {}: status={}, note={}", adminId, reportId, status, handleNote);

        // 如果确认违规且举报的是评论，直接删除该评论并通知评论发布者
        Long commentUserId = null;
        Long commentId = null;
        if ("RESOLVED".equals(status) && "COMMENT".equals(report.getTargetType())) {
            commentId = report.getTargetId();
            Comment comment = commentMapper.selectById(commentId);
            if (comment != null) {
                commentUserId = comment.getUserId();
                commentMapper.adminDelete(commentId);
                log.info("处理举报时删除违规评论: commentId={}, userId={}", commentId, commentUserId);
            } else {
                log.warn("举报的评论不存在，可能已被删除: commentId={}", commentId);
            }
        }

        // 通知举报人处理结果 —— 延迟到事务提交后执行
        String resultMsg = "RESOLVED".equals(status) ? "已确认违规并处理" : "经核查未发现违规，已驳回";
        Long reporterId = report.getReporterId();
        String targetType = report.getTargetType();
        String reason = report.getReason();
        Long finalCommentUserId = commentUserId;
        Long finalCommentId = commentId;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // 通知举报人
                notificationService.createNotification(
                        reporterId,
                        "REPORT_RESULT",
                        "举报处理结果",
                        "您对 " + targetType + " 的举报（" + reason + "）" + resultMsg +
                                (handleNote != null ? "。处理备注：" + handleNote : ""),
                        "REPORT",
                        reportId
                );

                // 通知评论发布者其评论因违规被删除
                if (finalCommentUserId != null) {
                    notificationService.createNotification(
                            finalCommentUserId,
                            "SYSTEM",
                            "评论被删除",
                            "您的评论因被举报违规已被管理员删除",
                            "COMMENT",
                            finalCommentId
                    );
                }
            }
        });
    }
}
