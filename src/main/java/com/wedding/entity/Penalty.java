package com.wedding.entity;

import com.wedding.enums.PenaltyType;
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
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

// 盘子:违规处罚记录 —— 对应表 t_penalty。
//
// 流水账而非状态机:每次处罚 INSERT 一条,只增不改;每条要能回答"罚谁(merchant_id) / 谁罚为何罚(operator_id / reason) / 怎么罚罚多久(penalty_type / start_time / end_time)"。
// target_id 仅在 penalty_type = SERVICE_OFF 时有值(指向被下架的 t_service.id);警告与封禁针对整个店铺,无需具体服务,故留空。
@Data
@Entity
@Table(name = "t_penalty")
public class Penalty {

    // 主键,数据库自增
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 被处罚商家,关联 t_merchant.id
    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    // 执行处罚的管理员,关联 t_user.id
    @Column(name = "operator_id", nullable = false)
    private Long operatorId;

    // 处罚类型:WARNING / SERVICE_OFF / BAN
    @Enumerated(EnumType.STRING)
    @Column(name = "penalty_type", length = 20, nullable = false)
    private PenaltyType penaltyType;

    // 处罚对象:仅"SERVICE_OFF 服务下架"时指向 t_service.id,其余类型为空
    @Column(name = "target_id")
    private Long targetId;

    // 处罚原因
    @Column(name = "reason", length = 500)
    private String reason;

    // 处罚生效时间
    @Column(name = "start_time")
    private LocalDateTime startTime;

    // 处罚结束时间,为空表示长期有效(如封禁)
    @Column(name = "end_time")
    private LocalDateTime endTime;

    // 处罚状态:1 生效中 / 0 已解除
    @Comment("1 生效中 / 0 已解除")
    @Column(name = "status", nullable = false, columnDefinition = "tinyint")
    private Integer status;

    // 记录创建时间 —— 应用生成(insert 时赋值,见数据库设计 6.5 节)
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 被罚商家名 —— 非持久化(@Transient),仅列表查询时由服务层回填,供前端直接展示。
    // 已删除/不存在的商家回填为 null,前端显示占位(如「商家#id」)。
    @Transient
    private String merchantName;
}
