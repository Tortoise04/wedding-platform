package com.wedding.entity;

import com.wedding.enums.DisputeResultType;
import com.wedding.enums.DisputeStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// 盘子:争议仲裁单 —— 对应表 t_dispute。
//
// 用户对商家不满时发起"平台介入",形成争议单;有独立状态机,与订单状态机互不占用 —— 用户发起争议时订单状态保持不变。
@Data
@Entity
@Table(name = "t_dispute")
public class Dispute {

    // 主键,数据库自增
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 争议单号,全局唯一,对外展示
    @Column(name = "dispute_no", length = 32, nullable = false)
    private String disputeNo;

    // 关联订单,一个订单最多引发一次平台介入
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    // 关联订单号(展示用),只从 t_order 回填,不落库
    @Transient
    private String orderNo;

    // 发起人(用户),关联 t_user.id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 被投诉商家,关联 t_merchant.id
    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    // 处理的管理员,关联 t_user.id(受理时赋值)
    @Column(name = "handler_id")
    private Long handlerId;

    // 用户诉求
    @Column(name = "reason", length = 500, nullable = false)
    private String reason;

    // 争议状态(独立状态机)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private DisputeStatus status;

    // 裁决结论:REFUND 退款 / CLOSE_ORDER 关闭订单 / PENALTY 处罚商家 / REJECT 驳回诉求
    @Enumerated(EnumType.STRING)
    @Column(name = "result_type", length = 20)
    private DisputeResultType resultType;

    // 退款金额(结论为退款时填写)
    @Column(name = "refund_amount", precision = 10, scale = 2)
    private BigDecimal refundAmount;

    // 裁决说明
    @Column(name = "result", length = 500)
    private String result;

    // 受理时间
    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    // 裁决完成时间
    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    // 发起时间 —— 应用生成(insert 时赋值,见数据库设计 6.5 节)
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 最近修改时间 —— 应用生成(insert / update 时赋值,见数据库设计 6.5 节)
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
