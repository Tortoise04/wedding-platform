package com.wedding.controller;

import com.wedding.common.Result;
import com.wedding.entity.Category;
import com.wedding.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 服务员:门户端 · 服务分类字典(路径 /api/categories,不贴注解 = 游客可访问)。
// 返回同样走统一 Result 结构(与其它接口一致)。
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    // GET /api/categories/1 → 按 id 查一个分类
    @GetMapping("/{id}")
    public Result<Category> getCategoryById(@PathVariable Long id) {
        return Result.success(categoryService.getCategoryById(id));
    }

    // GET /api/categories → 列出所有分类
    @GetMapping
    public Result<List<Category>> listCategories() {
        return Result.success(categoryService.listCategories());
    }
}
