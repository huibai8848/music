package com.example.music.mapper;

import com.example.music.entity.Playlist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 歌单 Mapper
 */
@Mapper
public interface PlaylistMapper {

    /** 根据 ID 查询 */
    Playlist selectById(@Param("id") Long id);

    /** 查询用户的歌单列表 */
    List<Playlist> selectByUserId(@Param("userId") Long userId);

    /** 查询用户的公开歌单列表（公开主页用） */
    List<Playlist> selectPublicByUserId(@Param("userId") Long userId);

    /** 查询公开歌单列表（分页） */
    List<Playlist> selectPublic(@Param("offset") int offset,
                                @Param("limit") int limit);

    /** 统计公开歌单总数 */
    long countPublic();

    /** 新增歌单 */
    int insert(Playlist playlist);

    /** 更新歌单 */
    int update(Playlist playlist);

    /** 更新歌曲计数 */
    int updateSongCount(@Param("id") Long id, @Param("count") int count);

    /** 统计所有歌单总数 */
    long countTotal();

    /** 删除歌单 */
    int deleteById(@Param("id") Long id);
}
