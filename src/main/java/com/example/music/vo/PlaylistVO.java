package com.example.music.vo;

import com.example.music.entity.Playlist;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 歌单展示 VO
 * <p>
 * 包含创建者信息和歌曲列表（可选）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistVO {

    private Long id;
    private Long userId;
    private String nickname;        // 创建者昵称
    private String title;
    private String description;
    private String coverUrl;
    private Boolean isPublic;
    private Integer songCount;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    /** 歌单内歌曲列表（仅在详情时返回） */
    private List<SongVO> songs;

    /**
     * 从 Playlist 实体构建基本 VO
     */
    public static PlaylistVO fromEntity(Playlist playlist) {
        if (playlist == null) return null;
        return PlaylistVO.builder()
                .id(playlist.getId())
                .userId(playlist.getUserId())
                .title(playlist.getTitle())
                .description(playlist.getDescription())
                .coverUrl(playlist.getCoverUrl())
                .isPublic(playlist.getIsPublic())
                .songCount(playlist.getSongCount())
                .createdTime(playlist.getCreatedTime())
                .updatedTime(playlist.getUpdatedTime())
                .build();
    }
}