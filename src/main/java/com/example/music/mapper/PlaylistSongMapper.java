package com.example.music.mapper;

import com.example.music.entity.PlaylistSong;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 歌单歌曲关联 Mapper
 */
@Mapper
public interface PlaylistSongMapper {

    /** 查询歌单内的所有歌曲 ID */
    List<Long> selectSongIdsByPlaylistId(@Param("playlistId") Long playlistId);

    /** 查询某首歌在歌单中的序号（用于排序） */
    Integer selectMaxSortOrder(@Param("playlistId") Long playlistId);

    /** 统计歌曲数量 */
    int countByPlaylistId(@Param("playlistId") Long playlistId);

    /** 检查歌曲是否已在歌单中 */
    int exist(@Param("playlistId") Long playlistId,
              @Param("songId") Long songId);

    /** 添加歌曲到歌单 */
    int insert(PlaylistSong playlistSong);

    /** 从歌单移除歌曲 */
    int delete(@Param("playlistId") Long playlistId,
               @Param("songId") Long songId);

    /** 清空歌单 */
    int deleteByPlaylistId(@Param("playlistId") Long playlistId);
}
