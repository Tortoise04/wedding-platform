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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

// 盘子:商家店铺 —— 对应表 t_merchant。
//
// 承载店铺资料 + 资质材料 + 入驻审核状态。
// ⚠️ 审核通过前账号 role 仍是 USER;通过后由管理员改为 MERCHANT(见入驻审核服务)。
@Data
@Entity
@Table(name = "t_merchant")
public class Merchant {

    // 主键,数据库自增
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 关联 t_user.id,一个账号最多一个店铺
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 店铺名称
    @Column(name = "shop_name", length = 100, nullable = false)
    private String shopName;

    // 经营范围
    @Column(name = "business_scope", length = 200)
    private String businessScope;

    // 联系方式
    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    // 店铺简介
    @Column(name = "description", length = 500)
    private String description;

    // 店铺 Logo 路径
    @Column(name = "logo", length = 255)
    private String logo;

    // 营业执照文件路径
    @Column(name = "license_url", length = 255)
    private String licenseUrl;

    // 服务资质证明文件路径
    @Column(name = "qualification_url", length = 255)
    private String qualificationUrl;

    // 审核状态:PENDING / APPROVED / REJECTED
    @Enumerated(EnumType.STRING)
    @Column(name = "audit_status", length = 20, nullable = false)
    private AuditStatus auditStatus;

    // 驳回原因
    @Column(name = "reject_reason", length = 255)
    private String rejectReason;

    // 审核时间
    @Column(name = "audit_time")
    private LocalDateTime auditTime;

    // 账号状态:1 启用 / 0 停用
    @Comment("账号状态：1 启用 / 0 停用")
    @Column(name = "status", nullable = false, columnDefinition = "tinyint")
    private Integer status;

    // 申请时间 —— 应用生成(insert 时赋值,见数据库设计 6.5 节)
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 最近修改时间 —— 应用生成(insert / update 时赋值,见数据库设计 6.5 节)
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
