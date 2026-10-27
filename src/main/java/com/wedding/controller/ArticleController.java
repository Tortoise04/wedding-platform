package com.wedding.controller;

import com.wedding.common.Result;
import com.wedding.entity.Article;
import com.wedding.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 服务员:门户端 · 婚庆资讯(需求 FR-A-04)。
//
// 不贴权限注解 → 游客可访问。
// ⚠️ /hot 与 /{id} 都能匹配 /api/articles/hot,但 Spring MVC 字面量优先于占位符,故一定走 hot(),不会被当成 id 报 400。
@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    // 已发布资讯列表(按发布时间倒序)
    @GetMapping
    public Result<List<Article>> list() {
        return Result.success(articleService.listPublished());
    }

    // 热门资讯(按阅读量倒序)
    @GetMapping("/hot")
    public Result<List<Article>> hot() {
        return Result.success(articleService.listHot());
    }

    // 资讯详情 —— 调这个接口会让阅读量 +1
    @GetMapping("/{id}")
    public Result<Article> detail(@PathVariable Long id) {
        return Result.success(articleService.getPublishedDetail(id));
    }
}
