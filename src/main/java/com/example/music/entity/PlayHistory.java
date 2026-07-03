package com.example.music.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 播放历史实体
 * <p>
 * 记录用户每次播放歌曲的行为，用于"最近播放"功能。
 * 每个用户最多保留 100 条记录（超出时删除最旧的）。
 */
@Data
public class PlayHistory {

    /** 主键 ID */
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 歌曲 ID */
    private Long songId;

    /** 播放时间 */
    private LocalDateTime playedTime;
}
