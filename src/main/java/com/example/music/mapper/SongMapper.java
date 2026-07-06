package com.example.music.mapper;

import com.example.music.entity.Song;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 歌曲 Mapper
 * <p>
 * 提供歌曲的 CRUD、搜索、筛选、播放量统计等功能。
 */
@Mapper
public interface SongMapper {

    /** 根据 ID 查询 */
    Song selectById(@Param("id") Long id);

    /** 分页查询歌曲列表 */
    List<Song> selectList(@Param("offset") int offset,
                          @Param("limit") int limit,
                          @Param("genre") String genre,
                          @Param("language") String language,
                          @Param("releaseYear") Integer releaseYear,
                          @Param("artistId") Long artistId,
                          @Param("albumId") Long albumId,
                          @Param("status") String status);

    /** 统计总数 */
    long countTotal(@Param("genre") String genre,
                    @Param("language") String language,
                    @Param("releaseYear") Integer releaseYear,
                    @Param("artistId") Long artistId,
                    @Param("albumId") Long albumId,
                    @Param("status") String status);

    /** 模糊搜索（歌曲名/艺人名/专辑名） */
    List<Song> search(@Param("keyword") String keyword,
                      @Param("offset") int offset,
                      @Param("limit") int limit);

    /** 搜索命中总数 */
    long countSearch(@Param("keyword") String keyword);

    /** 查询热门歌曲（按播放量排序） */
    List<Song> selectHot(@Param("limit") int limit);

    /** 查询某个艺人的歌曲（仅 ACTIVE，用于列表页） */
    List<Song> selectByArtistId(@Param("artistId") Long artistId);

    /** 查询某个艺人的所有歌曲（不限制状态，用于详情页展示所有歌曲） */
    List<Song> selectAllByArtistId(@Param("artistId") Long artistId);

    /** 查询某个上传者的歌曲 */
    List<Song> selectByUploaderId(@Param("uploaderId") Long uploaderId);

    /** 查询某个专辑的歌曲（仅 ACTIVE，用于列表页） */
    List<Song> selectByAlbumId(@Param("albumId") Long albumId);

    /** 查询某个专辑的所有歌曲（不限制状态，用于详情页展示所有歌曲） */
    List<Song> selectAllByAlbumId(@Param("albumId") Long albumId);

    /** 统计某个专辑的歌曲数量 */
    long countByAlbumId(@Param("albumId") Long albumId);

    /** 统计所有歌曲总播放量 */
    long sumPlayCount();

    /** 按风格统计歌曲数量分布 */
    List<java.util.Map<String, Object>> countByGenre();

    /** 新增歌曲 */
    int insert(Song song);

    /** 更新歌曲 */
    int update(Song song);

    /** 递增单曲播放量（每次播放 +1） */
    int incrementPlayCount(@Param("id") Long id);

    /** 更新播放量（批量，定时任务使用） */
    int batchUpdatePlayCount(@Param("updates") List<Song> updates);

    /** 删除歌曲 */
    int deleteById(@Param("id") Long id);

    /** 更新审核状态 */
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /** 审核歌曲（乐观锁：仅 PENDING 状态可被审核，防止并发重复审核） */
    int auditStatus(@Param("id") Long id, @Param("status") String status);
}