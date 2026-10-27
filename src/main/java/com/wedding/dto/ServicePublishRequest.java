package com.wedding.dto;

import lombok.Data;

import java.math.BigDecimal;

// 商家发布(或编辑)服务的请求体。
//
// ⚠️ 不含 merchant_id —— 商家身份由后端从 token 解析,绝不前端传(否则能冒充别家店铺发布)。
@Data
public class ServicePublishRequest {

    // 所属分类,关联 t_category.id
    private Long categoryId;

    // 服务名称
    private String name;

    // 封面图路径
    private String coverUrl;

    // 服务简介
    private String description;

    // 服务内容明细
    private String content;

    // 价格
    private BigDecimal price;

    // 计价单位:每场 / 每套 / 每天
    private String unit;
}
