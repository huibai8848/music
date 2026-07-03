package com.example.music.mapper;

import com.example.music.entity.Banner;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 轮播图 Mapper
 */
@Mapper
public interface BannerMapper {

    /** 根据 ID 查询 */
    Banner selectById(@Param("id") Long id);

    /** 查询所有轮播图（按 sort_order 升序） */
    List<Banner> selectList();

    /** 查询所有已启用的轮播图（前台展示用） */
    List<Banner> selectActive();

    /** 新增轮播图 */
    int insert(Banner banner);

    /** 更新轮播图 */
    int update(Banner banner);

    /** 删除轮播图 */
    int deleteById(@Param("id") Long id);
}
