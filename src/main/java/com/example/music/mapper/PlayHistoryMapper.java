package com.example.music.mapper;

import com.example.music.entity.PlayHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 播放历史 Mapper
 * <p>
 * 记录用户的播放历史，每次切换歌曲时插入一条记录。
 * 只保留最新 100 条（通过前端/后端定时清理或插入时限制）。
 */
@Mapper
public interface PlayHistoryMapper {

    /** 查询用户的播放历史（按播放时间降序，最多 100 条） */
    List<PlayHistory> selectByUserId(@Param("userId") Long userId,
                                     @Param("limit") int limit);

    /** 插入播放记录 */
    int insert(PlayHistory playHistory);

    /** 清空用户的播放历史 */
    int deleteByUserId(@Param("userId") Long userId);

    /** 统计用户的历史记录数 */
    int countByUserId(@Param("userId") Long userId);

    /** 删除超出限制的旧记录（保留最新的 N 条） */
    int deleteOldRecords(@Param("userId") Long userId,
                         @Param("keepCount") int keepCount);

    /** 统计指定日期的播放记录数 */
    long countByDate(@Param("date") LocalDate date);
}
