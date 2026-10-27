package com.wedding.spec;

import com.wedding.entity.WeddingService;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

// 服务的"查询条件工厂" —— 每个方法返回一条 Specification 条件。
//
// 条件要被组合使用(关键词 + 分类 + 价格区间……),集中在此,哪里要就调哪个,不重复写。
public class ServiceSpecs {

    // 门户搜索框:按关键词模糊匹配服务名
    public static Specification<WeddingService> keywordLike(String keyword) {
        return (root, query, cb) ->
                cb.like(root.get("name"), "%" + keyword + "%");
    }
    // 按分类筛选服务
    public static Specification<WeddingService> categoryEquals(Long categoryId) {
        return (root, query, cb) ->
                cb.equal(root.get("categoryId"), categoryId);
    }

    // 价格下限:只留 price >= priceMin 的服务
    public static Specification<WeddingService> priceGreaterThanOrEqual(BigDecimal priceMin) {
        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("price"), priceMin);
    }

    // 价格上限:只留 price <= priceMax 的服务
    public static Specification<WeddingService> priceLessThanOrEqual(BigDecimal priceMax) {
        return (root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("price"), priceMax);
    }


    // 商家端:只看本店铺的服务
    public static Specification<WeddingService> merchantEquals(Long merchantId) {
        return (root, query, cb) ->
                cb.equal(root.get("merchantId"), merchantId);
    }
    // 门户:只列已上架的服务(shelfStatus = 1)
    // ⚠️ shelfStatus 是实体属性名,不是数据库列名 shelf_status
    public static Specification<WeddingService> shelfOn() {
        return (root, query, cb) ->
                cb.equal(root.get("shelfStatus"), 1);
    }
}
