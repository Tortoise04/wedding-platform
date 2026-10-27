package com.wedding.service;

import com.wedding.dto.CreateArticleRequest;
import com.wedding.entity.Article;

import java.util.List;

// 厨师:婚庆资讯这道菜。
//
// 与公告一样一写多读:管理端看草稿、门户只看已发布。
// ★ 特殊点 getPublishedDetail:一次读请求却要改一个字段(阅读量 +1),坑在实现里。
public interface ArticleService {

    // 【管理端】发布资讯:adminId 来自 token
    Article publishArticle(Long adminId, CreateArticleRequest request);

    // 【管理端】资讯列表:status 传 null 就是全要(含草稿)
    List<Article> listForAdmin(Integer status);

    // 【门户】已发布资讯列表,按发布时间倒序
    List<Article> listPublished();

    // 【门户】热门资讯 —— 已发布里按阅读量倒序
    List<Article> listHot();

    // 【门户】资讯详情 —— 顺带把阅读量 +1
    Article getPublishedDetail(Long id);

    // 【管理端】上线 / 下线
    Article changeStatus(Long id, Integer status);
}
