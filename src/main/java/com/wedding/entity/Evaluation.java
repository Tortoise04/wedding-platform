package com.wedding.entity;

import com.wedding.enums.AuditStatus;
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

import java.time.LocalDateTime;

// 盘子:服务评价 —— 对应表 t_evaluation。
// 订单完成后用户评价,靠 order_id 唯一索引保证"一单一评"(幂等)。
@Data
@Entity
@Table(name = "t_evaluation")
public class Evaluation {

    // 主键,数据库自增
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 关联 t_order.id,一单一评
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    // 评价人,关联 t_user.id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 被评商家,关联 t_merchant.id
    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    // 被评服务,关联 t_service.id
    @Column(name = "service_id", nullable = false)
    private Long serviceId;

    // 评分,1 ~ 5
    @Comment("评分，1 ~ 5")
    @Column(name = "score", nullable = false, columnDefinition = "tinyint")
    private Integer score;

    // 文字评价
    @Column(name = "content", length = 1000)
    private String content;

    // 评价图片路径,多个以逗号分隔(最多 9 张)
    @Column(name = "images", length = 1000)
    private String images;

    // 评价视频地址,最多 1 个(可选)
    @Column(name = "video_url", length = 255)
    private String videoUrl;

    // 是否匿名评价:匿名时对外只显示「匿名用户」,不露姓名
    @Column(name = "anonymous", nullable = false, columnDefinition = "tinyint(1) default 0")
    private Boolean anonymous;

    // 审核状态 —— 这条评价的"公开闸门"。
    // 复用商家入驻 / 服务审核用的同一个 AuditStatus 枚举:PENDING 待审核 / APPROVED 已通过 / REJECTED 已驳回。
    // ⚠️ 列上【没有】数据库默认值:新评价必须由代码显式赋 PENDING,
    //    这是故意的 —— 万一哪里漏赋值,应当直接报错,而不是静默取个默认值把评价放出去。
    @Enumerated(EnumType.STRING)
    @Column(name = "audit_status", length = 20, nullable = false)
    private AuditStatus auditStatus;

    // 驳回原因(仅 REJECTED 时有值)
    @Column(name = "reject_reason", length = 255)
    private String rejectReason;

    // 审核时间(仅审核过才有值,由应用显式赋值,不用 @UpdateTimestamp 代劳)
    @Column(name = "audit_time")
    private LocalDateTime auditTime;

    // 商家回复内容(阶段 4 用)
    @Column(name = "reply_content", length = 500)
    private String replyContent;

    // 回复时间(阶段 4 用)
    @Column(name = "reply_time")
    private LocalDateTime replyTime;

    // 评价时间 —— 应用生成(insert 时赋值,见数据库设计 6.5 节)
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
