package com.example.music.mapper;

import com.example.music.entity.Artist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

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
}