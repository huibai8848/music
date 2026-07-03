package com.example.music.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 专辑实体
 * <p>
 * 对应数据库表 {@code album}，对歌曲按专辑进行分组管理。
 * 一张专辑属于一位艺人，包含多首歌曲。
 */
@Data
public class Album {

    /** 主键 ID */
    private Long id;

    /** 专辑名 */
    private String title;

    /** 所属艺人 ID */
    private Long artistId;

    /** 封面图 URL */
    private String coverUrl;

    /** 专辑简介 */
    private String description;

    /** 发行日期 */
    private LocalDate releaseDate;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 更新时间 */
    private LocalDateTime updatedTime;
}