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
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

// 盘子:订单状态流转记录 —— 对应表 t_order_status_log。
//
// 每次订单状态变更都写一条,记下"从哪来 / 到哪去 / 谁操作 / 备注"。
// 既满足"查看流转记录"需求,也是争议仲裁时还原履约过程的事实依据。
@Data
@Entity
@Table(name = "t_order_status_log")
public class OrderStatusLog {

    // 主键,数据库自增
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 关联 t_order.id
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    // 变更前状态(首次创建时为空)
    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private OrderStatus fromStatus;

    // 变更后状态
    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", length = 20, nullable = false)
    private OrderStatus toStatus;

    // 操作人,关联 t_user.id
    @Column(name = "operator_id", nullable = false)
    private Long operatorId;

    // 操作人身份:USER / MERCHANT / ADMIN
    @Column(name = "operator_role", length = 20, nullable = false)
    private String operatorRole;

    // 本次流转说明(如拒单原因)
    @Column(name = "remark", length = 255)
    private String remark;

    // 流转时间 —— 应用生成(insert 时赋值,见数据库设计 6.5 节)
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
