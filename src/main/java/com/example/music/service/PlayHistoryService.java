package com.example.music.service;

import java.util.List;
import java.util.Map;

/**
 * 播放历史服务接口
 */
public interface PlayHistoryService {

    /** 获取最近播放记录（返回 SongVO 列表） */
    List<Map<String, Object>> getHistory(Long userId);

    /** 记录播放历史 */
    void recordPlay(Long userId, Long songId);

    /** 清空播放历史 */
    void clearHistory(Long userId);
}
