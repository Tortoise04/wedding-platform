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

// 盘子:浏览记录 —— 对应表 t_browse_history。
// 用户点开服务详情时记一条,供"近期浏览"列表用;记录只管 user + service + 时间三件事。
@Data
@Entity
@Table(name = "t_browse_history")
public class BrowseHistory {

    // 主键,数据库自增
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 浏览人,关联 t_user.id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 被浏览服务,关联 t_service.id
    @Column(name = "service_id", nullable = false)
    private Long serviceId;

    // 浏览时间 —— 应用生成(insert 时赋值,见数据库设计 6.5 节)
    @CreationTimestamp
    @Column(name = "browse_time", nullable = false, updatable = false)
    private LocalDateTime browseTime;
}
