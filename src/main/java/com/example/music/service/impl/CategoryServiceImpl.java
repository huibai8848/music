package com.example.music.service.impl;

import com.example.music.entity.Category;
import com.example.music.mapper.CategoryMapper;
import com.example.music.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 分类服务实现
 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    @Override
    public List<Category> getCategoriesByType(String type) {
        return categoryMapper.selectByType(type);
    }

    @Override
    public Map<String, List<Category>> getAllCategories() {
        List<Category> all = categoryMapper.selectAll();
        return all.stream().collect(Collectors.groupingBy(Category::getType));
    }

    @Override
    public Category createCategory(Category category) {
        categoryMapper.insert(category);
        return category;
    }

    @Override
    public Category updateCategory(Category category) {
        categoryMapper.update(category);
        return categoryMapper.selectById(category.getId());
    }

    @Override
    public void deleteCategory(Long id) {
        categoryMapper.deleteById(id);
    }
}