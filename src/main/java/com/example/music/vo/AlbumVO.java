package com.example.music.vo;

import com.example.music.entity.Album;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 专辑展示 VO
 * <p>
 * 包含关联的艺人名和歌曲列表，用于专辑详情页展示。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlbumVO {

    private Long id;
    private String title;
    private Long artistId;
    private String artistName;       // 冗余：艺人名
    private String coverUrl;
    private String description;
    private LocalDate releaseDate;
    private LocalDateTime createdTime;

    /** 专辑内歌曲列表（仅在专辑详情时返回） */
    private List<SongVO> songs;

    /** 歌曲数量 */
    private int songCount;

    /**
     * 从 Album 实体构建基本 VO（不含关联名，需额外设置）
     */
    public static AlbumVO fromEntity(Album album) {
        if (album == null) return null;
        return AlbumVO.builder()
                .id(album.getId())
                .title(album.getTitle())
                .artistId(album.getArtistId())
                .coverUrl(album.getCoverUrl())
                .description(album.getDescription())
                .releaseDate(album.getReleaseDate())
                .createdTime(album.getCreatedTime())
                .build();
    }
}