package com.example.music.service.impl;

import com.example.music.entity.PlayHistory;
import com.example.music.entity.Song;
import com.example.music.mapper.PlayHistoryMapper;
import com.example.music.mapper.SongMapper;
import com.example.music.service.PlayHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 播放历史服务实现
 * <p>
 * 核心逻辑：
 * 1. 每次切换歌曲时记录播放历史
 * 2. 最多保留 100 条（超出时清理最旧的）
 * 3. 查询时返回带歌曲信息的 VO
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlayHistoryServiceImpl implements PlayHistoryService {

    private final PlayHistoryMapper playHistoryMapper;
    private final SongMapper songMapper;

    /** 最大保留记录数 */
    private static final int MAX_HISTORY = 100;

    @Override
    public List<Map<String, Object>> getHistory(Long userId) {
        List<PlayHistory> records = playHistoryMapper.selectByUserId(userId, MAX_HISTORY);

        return records.stream().map(record -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("playedTime", record.getPlayedTime());
            item.put("id", record.getId());

            // 关联查询歌曲信息
            Song song = songMapper.selectById(record.getSongId());
            if (song != null) {
                item.put("songId", song.getId());
                item.put("title", song.getTitle());
                item.put("coverUrl", song.getCoverUrl());
                item.put("duration", song.getDuration());
            } else {
                item.put("songId", record.getSongId());
                item.put("title", "歌曲已删除");
            }

            return item;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordPlay(Long userId, Long songId) {
        if (userId == null || songId == null) return;

        // 插入新记录
        PlayHistory record = new PlayHistory();
        record.setUserId(userId);
        record.setSongId(songId);
        playHistoryMapper.insert(record);

        // 检查是否超出限制，超出则清理最旧的
        int count = playHistoryMapper.countByUserId(userId);
        if (count > MAX_HISTORY) {
            playHistoryMapper.deleteOldRecords(userId, MAX_HISTORY);
        }

        log.debug("记录播放历史: userId={}, songId={}", userId, songId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearHistory(Long userId) {
        playHistoryMapper.deleteByUserId(userId);
        log.info("清空播放历史: userId={}", userId);
    }
}
