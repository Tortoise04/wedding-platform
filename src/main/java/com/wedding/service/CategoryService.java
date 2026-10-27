package com.wedding.service;

import com.wedding.entity.Category;

import java.util.List;

// 菜单:服务分类字典能做哪几件事。
public interface CategoryService {

    // 按 id 查分类
    Category getCategoryById(Long id);

    // 列出全部分类
    List<Category> listCategories();

    // 新增分类
    Category createCategory(String name);
}
