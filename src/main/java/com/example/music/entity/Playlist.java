package com.example.music.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 歌单实体
 * <p>
 * 用户创建的歌曲集合，支持公开/隐藏切换。
 * song_count 为冗余字段，用于列表展示避免 COUNT 子查询。
 */
@Data
public class Playlist {

    /** 主键 ID */
    private Long id;

    /** 创建者 ID */
    private Long userId;

    /** 歌单标题 */
    private String title;

    /** 歌单描述 */
    private String description;

    /** 封面图 URL */
    private String coverUrl;

    /** 是否公开 1=公开 0=隐藏 */
    private Boolean isPublic;

    /** 歌曲数量（冗余计数） */
    private Integer songCount;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 更新时间 */
    private LocalDateTime updatedTime;
}