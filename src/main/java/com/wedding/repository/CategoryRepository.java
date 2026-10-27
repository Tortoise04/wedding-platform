package com.wedding.repository;

import com.wedding.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

// 冷库:分类的存取。
// findById / findAll / save / delete 都继承自 JpaRepository,无需再写。
public interface CategoryRepository extends JpaRepository<Category, Long> {
}
