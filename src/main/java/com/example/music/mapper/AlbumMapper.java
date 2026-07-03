package com.example.music.mapper;

import com.example.music.entity.Album;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 专辑 Mapper
 */
@Mapper
public interface AlbumMapper {

    /** 根据 ID 查询 */
    Album selectById(@Param("id") Long id);

    /** 分页查询专辑列表 */
    List<Album> selectList(@Param("offset") int offset,
                           @Param("limit") int limit);

    /** 统计总数 */
    long countTotal();

    /** 按艺人查询专辑 */
    List<Album> selectByArtistId(@Param("artistId") Long artistId);

    /** 按标题 + 艺人精确查找 */
    Album selectByTitleAndArtist(@Param("title") String title,
                                  @Param("artistId") Long artistId);

    /** 按名称搜索 */
    List<Album> searchByName(@Param("keyword") String keyword);

    /** 新增专辑 */
    int insert(Album album);

    /** 更新专辑 */
    int update(Album album);

    /** 删除专辑 */
    int deleteById(@Param("id") Long id);
}