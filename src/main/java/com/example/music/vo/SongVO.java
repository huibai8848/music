package com.example.music.vo;

import com.example.music.entity.Song;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 歌曲展示 VO
 * <p>
 * 在前端歌曲列表/详情中展示，包含关联的艺人和专辑名称冗余字段，
 * 避免前端需要多次查询。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SongVO {

    private Long id;
    private String title;
    private Long artistId;
    private String artistName;       // 冗余：艺人名（列表展示用）
    private Long albumId;
    private String albumTitle;       // 冗余：专辑名
    private Integer duration;
    private String audioUrl;
    private String coverUrl;
    private String lyricUrl;
    private String genre;
    private String language;
    private Integer releaseYear;
    private String status;
    private Long playCount;
    private Long likeCount;       // 喜欢数
    private Long favoriteCount;   // 收藏数
    private LocalDateTime createdTime;
    private List<Long> categoryIds; // 分类 ID 列表

    /**
     * 从 Song 实体构建基本 VO（不含关联名，需额外设置）
     */
    public static SongVO fromEntity(Song song) {
        if (song == null) return null;
        return SongVO.builder()
                .id(song.getId())
                .title(song.getTitle())
                .artistId(song.getArtistId())
                .albumId(song.getAlbumId())
                .duration(song.getDuration())
                .audioUrl(song.getAudioUrl())
                .coverUrl(song.getCoverUrl())
                .lyricUrl(song.getLyricUrl())
                .genre(song.getGenre())
                .language(song.getLanguage())
                .releaseYear(song.getReleaseYear())
                .status(song.getStatus())
                .playCount(song.getPlayCount())
                .createdTime(song.getCreatedTime())
                .build();
    }
}