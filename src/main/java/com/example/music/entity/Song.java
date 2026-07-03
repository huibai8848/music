package com.example.music.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 歌曲实体
 * <p>
 * 对应数据库表 {@code song}，存储歌曲元数据、文件路径、播放量等信息。
 * 歌词 JSON 存储在 {@link #lyrics} 字段中，LRC 源文件路径存在 {@link #lyricUrl}。
 */
@Data
public class Song {

    /** 主键 ID */
    private Long id;

    /** 歌曲名 */
    private String title;

    /** 主艺人 ID */
    private Long artistId;

    /** 所属专辑 ID（允许为空） */
    private Long albumId;

    /** 时长（秒） */
    private Integer duration;

    /** 音频文件路径 */
    private String audioUrl;

    /** 封面图路径 */
    private String coverUrl;

    /** LRC 歌词文件路径 */
    private String lyricUrl;

    /** 解析后的歌词 JSON */
    private String lyrics;

    /** 风格（流行/摇滚/古典等） */
    private String genre;

    /** 语种（中文/英文/日文等） */
    private String language;

    /** 发行年份 */
    private Integer releaseYear;

    /** 审核状态: ACTIVE / PENDING / REJECTED */
    private String status;

    /** 上传者 ID（会员上传时非空） */
    private Long uploaderId;

    /** 播放量 */
    private Long playCount;

    /** 分类 ID 列表（非数据库字段，用于创建/更新时传递） */
    private java.util.List<Long> categoryIds;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 更新时间 */
    private LocalDateTime updatedTime;
}