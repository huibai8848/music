package com.example.music.service.impl;

import com.example.music.constant.ErrorCode;
import com.example.music.entity.Report;
import com.example.music.exception.BusinessException;
import com.example.music.mapper.ReportMapper;
import com.example.music.service.ReportService;
import com.example.music.vo.ReportVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 举报服务实现
 * <p>
 * 核心逻辑：
 * 1. 同一用户 24h 内不可重复举报同一内容
 * 2. 举报原因必须是预定义枚举值
 * 3. 提交频率限制（5 次/分钟，由 @RateLimit 注解控制）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportMapper reportMapper;

    /** 可用的举报原因 */
    private static final Set<String> VALID_REASONS = Set.of(
            "PORNOGRAPHY", "AD", "ABUSE", "COPYRIGHT", "OTHER"
    );

    /** 重复举报的时间窗口（小时） */
    private static final int DUPLICATE_WINDOW_HOURS = 24;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitReport(Long reporterId, String targetType, Long targetId,
                             String reason, String description) {
        // 1. 校验举报原因
        if (!VALID_REASONS.contains(reason)) {
            throw new BusinessException(ErrorCode.REPORT_REASON_INVALID);
        }

        // 2. 校验 24h 内是否已举报过同一内容
        Report latest = reportMapper.selectLatestByReporterAndTarget(
                reporterId, targetType, targetId);
        if (latest != null) {
            LocalDateTime lastTime = latest.getCreatedTime();
            if (lastTime != null && lastTime.plusHours(DUPLICATE_WINDOW_HOURS).isAfter(LocalDateTime.now())) {
                log.warn("重复举报: reporterId={}, targetType={}, targetId={}",
                        reporterId, targetType, targetId);
                throw new BusinessException(ErrorCode.REPORT_DUPLICATE);
            }
        }

        // 3. 创建举报
        Report report = new Report();
        report.setReporterId(reporterId);
        report.setTargetType(targetType);
        report.setTargetId(targetId);
        report.setReason(reason);
        report.setDescription(description);
        report.setStatus("PENDING");

        reportMapper.insert(report);
        log.info("提交举报: reporterId={}, targetType={}, targetId={}, reason={}",
                reporterId, targetType, targetId, reason);
    }

    @Override
    public Map<String, Object> getMyReports(Long reporterId, int page, int size) {
        int offset = (page - 1) * size;

        List<Report> list = reportMapper.selectByReporterId(reporterId, offset, size);
        long total = reportMapper.countByReporterId(reporterId);

        List<ReportVO> voList = list.stream()
                .map(ReportVO::fromEntity)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("records", voList);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }
}