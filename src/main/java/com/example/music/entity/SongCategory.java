package com.example.music.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 歌曲-分类关联表
 */
@Data
public class SongCategory {
    private Long id;
    private Long songId;
    private Long categoryId;
    private LocalDateTime createdTime;
}