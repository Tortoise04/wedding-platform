package com.wedding.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

// 服务检索的查询参数对象 —— 装用户筛选意图的小盒子。
//
// 条件会随需求增长,集中成对象后加字段不再改方法签名;Controller 直接声明它,Spring MVC 按名自动绑定 URL 参数。
@Data
public class ServiceSearchQuery {

    // 关键词,模糊匹配服务名称;不传表示不筛
    private String keyword;

    // 服务分类 id;不传表示不筛
    private Long categoryId;

    // 价格区间下限;不传表示不限
    private BigDecimal priceMin;

    // 价格区间上限;不传表示不限
    private BigDecimal priceMax;

    // 排序方式 —— 前端传的是业务名(newest 默认 / priceAsc / priceDesc / hot),
    // 具体映射到哪个列由 Service 决定,这样前端碰不到列名,也就没有注入口子。
    private String sort;

    // 页码 —— ⚠️ 从 0 开始(第 1 页是 0)
    private Integer page = 0;

    // 每页条数
    private Integer size = 10;

    // 按商家筛选(商家端查本店服务用);不传表示不筛
    private Long merchantId;

    // 可约日期(门户筛选用):该服务这一天必须有 status=1 的档期才算命中;不传表示不筛。
    // 只在「时间场地」里维护过档期的服务才会被命中 —— 这条筛选本身就是"当天真的能约"的声明。
    private LocalDate bookableDate;
}
