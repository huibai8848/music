package com.example.music.controller;

import com.example.music.dto.ReportDTO;
import com.example.music.service.ReportService;
import com.example.music.utils.RequestContext;
import com.example.music.vo.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 举报控制器（用户端）
 * <p>
 * 提供提交举报和查询我的举报记录接口。
 * 管理端的举报处理接口在 Admin 模块中实现。
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * 提交举报
     * <p>
     * 同一用户 24h 内对同一内容不可重复举报。
     *
     * @param dto 举报参数
     */
    @PostMapping
    public R<Object> submitReport(@Valid @RequestBody ReportDTO dto) {
        Long userId = RequestContext.getUserId();
        if (userId == null) {
            return R.fail("请先登录");
        }

        reportService.submitReport(
                userId,
                dto.getTargetType().toUpperCase(),
                dto.getTargetId(),
                dto.getReason(),
                dto.getDescription()
        );
        return R.ok("举报已提交，请等待处理");
    }

    /**
     * 获取我的举报记录（分页，按时间降序）
     *
     * @param page 页码，从 1 开始
     * @param size 每页条数，默认 20
     */
    @GetMapping
    public R<Map<String, Object>> getMyReports(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = RequestContext.getUserId();
        if (userId == null) {
            return R.fail("请先登录");
        }
        return R.ok(reportService.getMyReports(userId, page, size));
    }
}