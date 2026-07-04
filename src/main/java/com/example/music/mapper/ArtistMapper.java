package com.example.music.mapper;

import com.example.music.entity.Artist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 艺人 Mapper
 */
@Mapper
public interface ArtistMapper {

    /** 根据 ID 查询 */
    Artist selectById(@Param("id") Long id);

    /** 分页查询艺人列表 */
    List<Artist> selectList(@Param("offset") int offset,
                            @Param("limit") int limit);

    /** 统计总数 */
    long countTotal();

    /** 按名称精确查找 */
    Artist selectByName(@Param("name") String name);

    /** 按名称搜索 */
    List<Artist> searchByName(@Param("keyword") String keyword,
                              @Param("offset") int offset,
                              @Param("limit") int limit);

    /** 统计搜索结果数 */
    long countSearch(@Param("keyword") String keyword);

    /** 新增艺人 */
    int insert(Artist artist);

    /** 更新艺人 */
    int update(Artist artist);

    /** 删除艺人 */
    int deleteById(@Param("id") Long id);

    // ==================== 公开查询（仅包含有 ACTIVE 歌曲的艺人） ====================

    /** 分页查询有 ACTIVE 歌曲的艺人 */
    List<Artist> selectListWithActiveSongs(@Param("offset") int offset,
                                           @Param("limit") int limit);

    /** 统计有 ACTIVE 歌曲的艺人总数 */
    long countWithActiveSongs();

    /** 按名称搜索有 ACTIVE 歌曲的艺人 */
    List<Artist> searchByNameWithActiveSongs(@Param("keyword") String keyword,
                                             @Param("offset") int offset,
                                             @Param("limit") int limit);

    /** 统计有 ACTIVE 歌曲的艺人搜索结果数 */
    long countSearchWithActiveSongs(@Param("keyword") String keyword);

    /** 批量查询艺人的 ACTIVE 歌曲数量，返回 [{artist_id, song_count}, ...] */
    List<Map<String, Object>> selectSongCountsByArtistIds(@Param("list") List<Long> artistIds);
}