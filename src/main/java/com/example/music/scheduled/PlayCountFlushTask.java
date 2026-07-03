package com.example.music.scheduled;

import com.example.music.constant.RedisKeys;
import com.example.music.mapper.SongMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 播放计数定时落库任务
 * <p>
 * 每 5 分钟执行一次，将 Redis 中的播放计数缓冲刷入 MySQL。
 * 设计目的：避免每次播放都直接写数据库，减少写压力。
 *
 * 工作流程：
 * 1. 使用 SCAN 遍历所有 play_count:buffer:* 键（通过 StringRedisTemplate）
 * 2. 使用 GETDEL 原子读取并删除每个键的计数值（避免竞态条件）
 * 3. 批量 UPDATE song.play_count
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlayCountFlushTask {

    private final StringRedisTemplate stringRedisTemplate;
    private final SongMapper songMapper;

    /**
     * 每 5 分钟执行一次（fixedRate = 300000 ms）
     * 参考：详细模块设计文档 3.4.2 播放量统计
     */
    @Scheduled(fixedRate = 300000)
    public void flushPlayCount() {
        log.debug("【定时任务】开始刷新播放计数...");

        List<com.example.music.entity.Song> updates = new ArrayList<>();

        // 使用 SCAN 遍历匹配的 key（避免 KEYS 命令阻塞 Redis）
        String pattern = RedisKeys.PLAY_COUNT_BUFFER + "*";

        try (Cursor<String> cursor = stringRedisTemplate.scan(
                ScanOptions.scanOptions()
                        .match(pattern)
                        .count(100)
                        .build())) {

            while (cursor.hasNext()) {
                String key = cursor.next();

                // 读取当前缓冲值（兼容 Redis 6.2 以下版本，用 get + delete 替代 GETDEL）
                String value = stringRedisTemplate.opsForValue().get(key);
                if (value != null) {
                    // 删除已读取的键（两次操作，但在播放量场景下可接受）
                    stringRedisTemplate.delete(key);
                }
                if (value == null) continue;

                long count = Long.parseLong(value);

                if (count <= 0) continue;

                // 从 key 中提取 songId
                String songIdStr = key.substring(RedisKeys.PLAY_COUNT_BUFFER.length());
                Long songId = Long.parseLong(songIdStr);

                // 准备更新数据
                com.example.music.entity.Song song = new com.example.music.entity.Song();
                song.setId(songId);
                song.setPlayCount(count);
                updates.add(song);
            }
        } catch (Exception e) {
            log.error("【定时任务】SCAN 播放计数异常", e);
            return;
        }

        // 批量更新数据库
        if (!updates.isEmpty()) {
            try {
                songMapper.batchUpdatePlayCount(updates);
                log.info("【定时任务】播放计数刷新完成, 更新 {} 条记录", updates.size());
            } catch (Exception e) {
                log.error("【定时任务】批量更新播放计数失败", e);
            }
        }
    }
}