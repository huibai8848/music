package com.example.music.vo;

import com.example.music.entity.Artist;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 艺人展示 VO
 * <p>
 * 包含歌曲数量和作品列表，用于艺人详情页展示。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArtistVO {

    private Long id;
    private String name;
    private String avatar;
    private String bio;
    private String country;
    private LocalDateTime createdTime;

    /** 歌曲数量 */
    private int songCount;

    /** 作品列表（仅在艺人详情时返回） */
    private List<SongVO> songs;

    /** 专辑列表（仅在艺人详情时返回） */
    private List<AlbumVO> albums;

    /**
     * 从 Artist 实体构建基本 VO
     */
    public static ArtistVO fromEntity(Artist artist) {
        if (artist == null) return null;
        return ArtistVO.builder()
                .id(artist.getId())
                .name(artist.getName())
                .avatar(artist.getAvatar())
                .bio(artist.getBio())
                .country(artist.getCountry())
                .createdTime(artist.getCreatedTime())
                .build();
    }
}