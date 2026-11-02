package com.wedding.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// 盘子:咨询会话 —— 对应表 t_consultation。
// 只存会话级信息(列表页只需读本表);进入会话后再读 t_consult_message。
// 同一对用户与商家只有一个会话(uk_user_merchant),重复发起时复用而非新建。
@Data
@Entity
@Table(name = "t_consultation")
public class Consultation {

    // 主键,数据库自增
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 咨询用户,关联 t_user.id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 被咨询商家,关联 t_merchant.id
    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    // 咨询针对的服务(可为空:直接咨询店铺而非某个服务)
    @Column(name = "service_id")
    private Long serviceId;

    // 最近一次来源订单(可为空:从服务详情或会话列表发起的咨询没有订单)
    // 只记「最近一次」——同一对用户与商家始终只有一条会话,新一次的来意覆盖旧的。
    @Column(name = "order_id")
    private Long orderId;

    // 最后一条消息摘要(列表页展示用)
    @Column(name = "last_message", length = 255)
    private String lastMessage;

    // 最后一条消息时间(列表排序依据)
    @Column(name = "last_time")
    private LocalDateTime lastTime;

    // 用户未读数
    @Column(name = "user_unread", nullable = false)
    private Integer userUnread;

    // 商家未读数
    @Column(name = "merchant_unread", nullable = false)
    private Integer merchantUnread;

    // 会话创建时间 —— 应用生成(insert 时赋值,见数据库设计 6.5 节)
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 咨询方用户名 —— 不落库(t_consultation 没有这一列),商家端列表/会话头展示用。
    // 由 ConsultationServiceImpl.listMerchantConsultations 按 userId 回查 t_user 填上,
    // 否则商家只能看到「用户 #3」,不知道在跟谁聊。
    @Transient
    private String userName;

    // ↓ 以下 6 个只用于「来源卡片」:会话头/列表要显示「你是从哪项服务或哪笔订单点进来的」,
    //   这几个值散在 t_service 与 t_order 里,列表页不可能为了 20 条会话再查 40 次。
    //   它们一律不落库(t_consultation 没有这些列),由 ConsultationServiceImpl 按 id 批量回填;
    //   服务或订单取不到就留 null,前端显示占位,不为一条脏数据把整页拉挂。

    // 来源服务名(服务已被删除 / 会话没绑服务时为 null)
    @Transient
    private String sourceServiceName;

    // 来源服务封面图路径
    @Transient
    private String sourceServiceCover;

    // 来源服务价格
    @Transient
    private BigDecimal sourceServicePrice;

    // 来源订单号(对外展示用)
    @Transient
    private String sourceOrderNo;

    // 来源订单金额
    @Transient
    private BigDecimal sourceOrderAmount;

    // 来源订单状态中文名(待审核 / 已确认 / 履约中 / 已完成 / 已取消)
    @Transient
    private String sourceOrderStatus;
}
