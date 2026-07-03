package com.example.music.service;

import com.example.music.entity.Category;

import java.util.List;
import java.util.Map;

/**
 * 分类服务接口
 */
public interface CategoryService {

    /** 按类型获取分类列表 */
    List<Category> getCategoriesByType(String type);

    /** 获取所有分类（按类型分组） */
    Map<String, List<Category>> getAllCategories();

    /** 新增分类 */
    Category createCategory(Category category);

    /** 更新分类 */
    Category updateCategory(Category category);

    /** 删除分类 */
    void deleteCategory(Long id);
}