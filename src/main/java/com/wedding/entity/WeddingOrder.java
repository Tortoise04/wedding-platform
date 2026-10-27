package com.wedding.entity;

import com.wedding.enums.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// 盘子:一个"订单"长什么样 —— 对应表 t_order。
//
// ⚠️ 类名不叫 Order:避免与 Spring 的 @Order / Sort.Order 撞名(同 WeddingService)。
// status 承载订单状态机,取值见《系统设计》4.3:PENDING 待审核 / CONFIRMED 已确认 / IN_PROGRESS 履约中 / COMPLETED 已完成 / CANCELLED 已取消。
@Data
@Entity
@Table(name = "t_order")
public class WeddingOrder {

    // 主键,数据库自增
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 订单号,全局唯一,对外展示
    @Column(name = "order_no", length = 32, nullable = false)
    private String orderNo;

    // 下单用户,关联 t_user.id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 接单商家,关联 t_merchant.id
    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    // 所购服务,关联 t_service.id
    @Column(name = "service_id", nullable = false)
    private Long serviceId;

    // 婚礼日期
    @Column(name = "wedding_date")
    private LocalDate weddingDate;

    // 宾客人数
    @Column(name = "guest_count")
    private Integer guestCount;

    // 预算区间下限
    @Column(name = "budget_min", precision = 10, scale = 2)
    private BigDecimal budgetMin;

    // 预算区间上限
    @Column(name = "budget_max", precision = 10, scale = 2)
    private BigDecimal budgetMax;

    // 数量
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    // 订单金额
    @Column(name = "amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    // 备注信息
    @Column(name = "remark", length = 500)
    private String remark;

    // 订单状态(状态机)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private OrderStatus status;

    // 取消原因 / 商家拒单原因
    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;

    // 接单确认时间
    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    // 服务完成时间
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // 取消时间
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    // 售后状态:null 未发起 / 1 处理中 / 2 已完结
    // ⚠️ Integer 映射 tinyint 必须显式写 columnDefinition,否则 Hibernate 按 integer 推导,每次启动悄悄 alter 列类型
    @Column(name = "after_sale_status", columnDefinition = "tinyint")
    @Comment("售后状态：1 处理中 / 2 已完结")
    private Integer afterSaleStatus;

    // 售后处理说明,给用户看的
    @Column(name = "after_sale_note", length = 500)
    @Comment("售后处理说明")
    private String afterSaleNote;

    // 售后处理时间
    @Column(name = "after_sale_at")
    private LocalDateTime afterSaleAt;

    // 下单时间 —— 应用生成(insert 时赋值,见数据库设计 6.5 节)
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 最近修改时间 —— 应用生成(insert / update 时赋值,见数据库设计 6.5 节)
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
