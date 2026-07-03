package com.example.music.mapper;

import com.example.music.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 分类 Mapper
 */
@Mapper
public interface CategoryMapper {

    /** 根据 ID 查询 */
    Category selectById(@Param("id") Long id);

    /** 按类型查询分类列表 */
    List<Category> selectByType(@Param("type") String type);

    /** 查询所有分类 */
    List<Category> selectAll();

    /** 新增分类 */
    int insert(Category category);

    /** 更新分类 */
    int update(Category category);

    /** 删除分类 */
    int deleteById(@Param("id") Long id);
}