package com.wedding.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// 盘子:一个"服务"长什么样 —— 对应表 t_service。
//
// ⚠️ 类名不叫 Service:与 Spring 的 org.springframework.stereotype.Service 注解同名会导致 import 冲突,故改叫 WeddingService 再用 @Table 映射。
// ⚠️ 每个字段都显式标长度/精度/类型(《数据库设计说明书》6.4 节):不标的话 Hibernate 会按 Java 默认长度改表(如 String 变 varchar(255))、并清空列注释。
@Data
@Entity
@Table(name = "t_service")
public class WeddingService {

    // 主键,数据库自增
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 所属商家,关联 t_merchant.id
    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    // 所属分类,关联 t_category.id
    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    // 服务名称(关键词检索字段之一)
    @Column(name = "name", length = 100, nullable = false)
    private String name;

    // 封面图路径
    @Column(name = "cover_url", length = 255)
    private String coverUrl;

    // 服务简介(关键词检索字段之一)
    @Column(name = "description", length = 500)
    private String description;

    // 服务内容明细 —— 长文本。
    // ⚠️ 三个注解缺一不可(D-21):columnDefinition = "text" 对上列类型;length = 65535 关掉启动噪声(Hibernate 只认 length,少了它每次启动都发一句 alter table);@Comment 补回被 Hibernate 抹掉的列注释。
    @Comment("服务内容明细")
    @Column(name = "content", columnDefinition = "text", length = 65535)
    private String content;

    // 价格 —— DECIMAL(10,2),Java 侧必须用 BigDecimal(不能用 double,浮点会有精度误差)
    @Column(name = "price", precision = 10, scale = 2, nullable = false)
    private BigDecimal price;

    // 计价单位:每场 / 每套 / 每天
    @Column(name = "unit", length = 20)
    private String unit;

    // 内容审核状态:PENDING / APPROVED / REJECTED
    @Column(name = "audit_status", length = 20, nullable = false)
    private String auditStatus;

    // 1 上架 / 0 下架
    @Comment("1 上架 / 0 下架")
    @Column(name = "shelf_status", nullable = false, columnDefinition = "tinyint")
    private Integer shelfStatus;

    // 浏览量(排序依据)
    @Column(name = "view_count", nullable = false)
    private Integer viewCount;

    // 成交量(排序依据)
    @Column(name = "sales_count", nullable = false)
    private Integer salesCount;

    // 平均评分(排序依据) —— DECIMAL(3,2)
    @Column(name = "score", precision = 3, scale = 2, nullable = false)
    private BigDecimal score;

    // 发布时间 —— 应用生成(见数据库设计 6.5 节)。
    // ⚠️ 原用 insertable = false 交给数据库默认值,插完读不回值,接口响应里 createdAt 会是 null(D-17);改为 @CreationTimestamp 由应用赋值。
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 最近修改时间 —— 应用生成(insert / update 时赋值,见数据库设计 6.5 节)
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
