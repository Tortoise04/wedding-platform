package com.wedding.controller;

import com.wedding.annotation.RequireAdmin;
import com.wedding.common.Result;
import com.wedding.dto.CreateArticleRequest;
import com.wedding.entity.Article;
import com.wedding.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 服务员:管理端 · 婚庆资讯维护(需求 FR-A-04)。
//
// 列表(含草稿) / 发布或存草稿 / 上线下线,与 AdminAnnouncementController 同构。
@RestController
@RequestMapping("/api/admin/articles")
@RequiredArgsConstructor
public class AdminArticleController {

    private final ArticleService articleService;

    // 资讯列表:status 不传 = 草稿和已发布全要
    @RequireAdmin
    @GetMapping
    public Result<List<Article>> list(@RequestParam(required = false) Integer status) {
        return Result.success(articleService.listForAdmin(status));
    }

    // 发布资讯(或存草稿)
    @RequireAdmin
    @PostMapping
    public Result<Article> publish(@RequestBody CreateArticleRequest request,
                                   @RequestAttribute("userId") Long adminId) {
        return Result.success(articleService.publishArticle(adminId, request));
    }

    // 上线(status=1)/ 下线(status=0)
    @RequireAdmin
    @PutMapping("/{id}/status")
    public Result<Article> changeStatus(@PathVariable Long id,
                                        @RequestParam Integer status) {
        return Result.success(articleService.changeStatus(id, status));
    }
}
