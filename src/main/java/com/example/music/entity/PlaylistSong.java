package com.example.music.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 歌单歌曲关联实体
 * <p>
 * 维护歌单和歌曲的多对多关系。
 */
@Data
public class PlaylistSong {

    /** 主键 ID */
    private Long id;

    /** 歌单 ID */
    private Long playlistId;

    /** 歌曲 ID */
    private Long songId;

    /** 排序序号 */
    private Integer sortOrder;

    /** 添加时间 */
    private LocalDateTime addedTime;
}