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

import java.time.LocalDate;
import java.time.LocalDateTime;

// 盘子:一个服务的"可预约档期" —— 对应表 t_service_schedule。
//
// 一个服务可有多天档期(如司仪 10 月 1 号、10 月 3 号有空),通过 service_id 挂在服务下,一天一条。
@Data
@Entity
@Table(name = "t_service_schedule")
public class ServiceSchedule {

    // 主键,数据库自增
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 所属服务,关联 t_service.id
    @Column(name = "service_id", nullable = false)
    private Long serviceId;

    // 可预约日期
    @Column(name = "schedule_date", nullable = false)
    private LocalDate scheduleDate;

    // 1 可预约 / 0 已占用或不可用
    @Comment("1 可预约 / 0 已占用或不可用")
    @Column(name = "status", nullable = false, columnDefinition = "tinyint")
    private Integer status;

    // 创建时间 —— 应用生成(insert 时赋值,见数据库设计 6.5 节)
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
