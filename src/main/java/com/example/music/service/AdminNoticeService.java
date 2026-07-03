package com.example.music.service;

import com.example.music.dto.NoticeDTO;
import com.example.music.vo.SystemNoticeVO;

import java.util.List;
import java.util.Map;

/**
 * 管理后台系统公告服务接口
 * <p>
 * 提供公告的 CRUD 管理功能。
 */
public interface AdminNoticeService {

    /** 分页查询公告 */
    Map<String, Object> listNotices(String type, int page, int size);

    /** 获取单个公告 */
    SystemNoticeVO getNotice(Long id);

    /** 新增公告 */
    SystemNoticeVO createNotice(NoticeDTO dto);

    /** 更新公告 */
    SystemNoticeVO updateNotice(Long id, NoticeDTO dto);

    /** 删除公告 */
    void deleteNotice(Long id);

    /** 获取所有已启用的公告（前台展示用） */
    List<SystemNoticeVO> getActiveNotices();
}
