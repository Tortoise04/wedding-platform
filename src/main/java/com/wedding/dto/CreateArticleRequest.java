package com.wedding.dto;

import lombok.Data;

// 管理端 · 发布婚庆资讯的入参。
//
// 与发布公告同构:无 publisherId(从 token 取)、无 publishTime(服务端盖章)。
// 也无 viewCount —— 阅读量由读者点出来,发布时预置一个等于数据造假。
@Data
public class CreateArticleRequest {

    // 资讯标题(必填)
    private String title;

    // 封面图路径
    private String coverUrl;

    // 正文
    private String content;

    // 作者 / 来源
    private String author;

    // 1 直接发布 / 0 存草稿(不传按 0 处理)
    private Integer status;
}
