package com.example.music.controller;

import com.example.music.entity.Category;
import com.example.music.service.CategoryService;
import com.example.music.vo.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 分类控制器
 * <p>
 * 查询歌曲分类（风格/语种/年代），用于前端筛选条件展示。
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 按类型获取分类列表
     *
     * @param type 分类类型: GENRE / LANGUAGE / YEAR
     */
    @GetMapping
    public R<Object> getCategories(@RequestParam(required = false) String type) {
        if (type != null && !type.isEmpty()) {
            return R.ok(categoryService.getCategoriesByType(type));
        }
        // 不传 type 时按类型分组返回
        return R.ok(categoryService.getAllCategories());
    }
}