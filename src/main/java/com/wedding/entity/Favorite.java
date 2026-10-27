package com.wedding.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

// 盘子:收藏 —— 对应表 t_favorite。
// 用户收藏心仪服务,靠 (user_id, service_id) 唯一索引防重复收藏(幂等)。
@Data
@Entity
@Table(name = "t_favorite")
public class Favorite {

    // 主键,数据库自增
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 收藏人,关联 t_user.id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 被收藏服务,关联 t_service.id
    @Column(name = "service_id", nullable = false)
    private Long serviceId;

    // 收藏时间 —— 应用生成(insert 时赋值,见数据库设计 6.5 节)
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
