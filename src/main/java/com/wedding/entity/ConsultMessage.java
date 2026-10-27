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

import java.time.LocalDateTime;

// 盘子:咨询消息 —— 对应表 t_consult_message。
// 一条消息挂在某个会话下,sender_role 区分是用户还是商家发的,双方消息并列按时间正序呈现。
@Data
@Entity
@Table(name = "t_consult_message")
public class ConsultMessage {

    // 主键,数据库自增
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 所属会话,关联 t_consultation.id
    @Column(name = "consultation_id", nullable = false)
    private Long consultationId;

    // 发送人,关联 t_user.id
    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    // 发送人身份:USER / MERCHANT
    @Column(name = "sender_role", length = 20, nullable = false)
    private String senderRole;

    // 消息内容
    @Column(name = "content", length = 1000, nullable = false)
    private String content;

    // 1 已读 / 0 未读
    @Comment("1 已读 / 0 未读")
    @Column(name = "is_read", nullable = false, columnDefinition = "tinyint")
    private Integer isRead;

    // 发送时间 —— 应用生成(insert 时赋值,见数据库设计 6.5 节)
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
