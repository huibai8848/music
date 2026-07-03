package com.example.music.mapper;

import com.example.music.entity.SongCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 歌曲-分类关联 Mapper
 */
@Mapper
public interface SongCategoryMapper {
    int insert(SongCategory songCategory);
    int deleteBySongId(@Param("songId") Long songId);
    int deleteByCategoryId(@Param("categoryId") Long categoryId);
    List<SongCategory> selectBySongId(@Param("songId") Long songId);
    List<SongCategory> selectByCategoryId(@Param("categoryId") Long categoryId);
    List<Long> selectCategoryIdsBySongId(@Param("songId") Long songId);
    int insertBatch(@Param("list") List<SongCategory> list);
    int countByCategoryId(@Param("categoryId") Long categoryId);
}