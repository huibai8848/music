package com.example.music.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 艺人实体
 * <p>
 * 对应数据库表 {@code artist}，存储歌手/乐队等艺人的基本信息。
 * 艺人可以拥有多张专辑和多首歌曲。
 */
@Data
public class Artist {

    /** 主键 ID */
    private Long id;

    /** 艺人名 */
    private String name;

    /** 头像 URL */
    private String avatar;

    /** 艺人简介 */
    private String bio;

    /** 国籍/地区 */
    private String country;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 更新时间 */
    private LocalDateTime updatedTime;
}