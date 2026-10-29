package com.wedding.spec;

import com.wedding.entity.ServiceSchedule;
import com.wedding.entity.WeddingService;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    // 门户:按可约日期筛 —— 该服务这一天必须有一条 status=1 的档期。
    // 用 exists 子查询而不是 join:同一天可能有多条(上午/下午),join 会让一个服务在结果里重复出现,
    // 分页总数也跟着翻倍;exists 只判断"有没有",一个服务最多命中一次。
    // ⚠️ 严格匹配(日期相等且 status=1),不是"这天之后有没有档期" —— 否则筛了等于没筛。
    public static Specification<WeddingService> hasBookableDate(LocalDate date) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<ServiceSchedule> s = sub.from(ServiceSchedule.class);
            sub.select(s.get("id")).where(
                    cb.equal(s.get("serviceId"), root.get("id")),      // 档期属于这个服务
                    cb.equal(s.get("scheduleDate"), date),             // 就是这一天
                    cb.equal(s.get("status"), 1));                     // 且这一天仍可约
            return cb.exists(sub);
        };
    }
}
