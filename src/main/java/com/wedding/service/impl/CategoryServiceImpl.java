package com.wedding.service.impl;

import com.wedding.entity.Category;
import com.wedding.exception.BusinessException;
import com.wedding.repository.CategoryRepository;
import com.wedding.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

// 厨师:分类这道菜(门户读取分类 + 管理端新增分类)。
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    // 分类仓储
    private final CategoryRepository categoryRepository;

    // 按 id 查分类:查不到直接报错,供详情页与关联校验复用。
    @Override
    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException("分类不存在: " + id));
    }

    // 分类列表:全量返回,门户筛选用。
    @Override
    public List<Category> listCategories() {
        return categoryRepository.findAll();
    }

    // 新增分类:闸门是分类名非空,仅管理端使用。
    @Override
    public Category createCategory(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("分类名不能为空");
        }
        Category category = new Category();
        category.setName(name);
        return categoryRepository.save(category);
    }
}
