package com.wedding.entity;

import com.wedding.enums.EvidenceType;
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

// 盘子:争议举证材料 —— 对应表 t_dispute_evidence。
//
// submitter_role 区分材料由用户还是商家提交,是仲裁时"双方材料并列呈现"的前提。
@Data
@Entity
@Table(name = "t_dispute_evidence")
public class DisputeEvidence {

    // 主键,数据库自增
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 所属争议,关联 t_dispute.id
    @Column(name = "dispute_id", nullable = false)
    private Long disputeId;

    // 提交人,关联 t_user.id
    @Column(name = "submitter_id", nullable = false)
    private Long submitterId;

    // 提交方身份:USER / MERCHANT
    @Column(name = "submitter_role", length = 20, nullable = false)
    private String submitterRole;

    // 材料类型:IMAGE 图片 / TEXT 文字
    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_type", length = 20, nullable = false)
    private EvidenceType evidenceType;

    // 文字说明 / 文字举证
    @Column(name = "content", length = 1000)
    private String content;

    // 凭证文件路径
    @Column(name = "file_url", length = 255)
    private String fileUrl;

    // 提交时间 —— 应用生成(insert 时赋值,见数据库设计 6.5 节)
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
