package com.example.music.controller;

import com.example.music.service.AdminNoticeService;
import com.example.music.vo.R;
import com.example.music.vo.SystemNoticeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 前台公告控制器
 * <p>
 * 提供公开的系统公告读取接口（无需登录），仅返回已启用的公告。
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class NoticeController {

    private final AdminNoticeService adminNoticeService;

    /**
     * 获取所有已启用的公告
     * <p>
     * 首页展示用，仅返回 is_active = TRUE 的公告，按创建时间降序排列。
     */
    @GetMapping("/notices")
    public R<List<SystemNoticeVO>> getActiveNotices() {
        return R.ok(adminNoticeService.getActiveNotices());
    }
}
