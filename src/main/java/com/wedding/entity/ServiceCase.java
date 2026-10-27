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

// 盘子:一个服务的"案例素材" —— 对应表 t_service_case。
//
// 一个服务有多组案例(图片 + 说明),通过 service_id 挂在服务下;查服务详情时按 service_id 全部捞出。
@Data
@Entity
@Table(name = "t_service_case")
public class ServiceCase {

    // 主键,数据库自增
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 所属服务,关联 t_service.id
    @Column(name = "service_id", nullable = false)
    private Long serviceId;

    // 案例图片路径
    @Column(name = "image_url", length = 255, nullable = false)
    private String imageUrl;

    // 案例视频路径(可空):一组案例除了图,还能挂一段现场视频,详情页点开即播
    @Column(name = "video_url", length = 255)
    private String videoUrl;

    // 案例标题
    @Column(name = "title", length = 100)
    private String title;

    // 案例说明
    @Column(name = "description", length = 500)
    private String description;

    // 案例分类:场布 / 仪式 / 宴席 / 外景 / 其他,可为空
    @Column(name = "category", length = 20)
    @Comment("案例分类：场布 / 仪式 / 宴席 / 外景 / 其他")
    private String category;

    // 展示排序,数值越小越靠前
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    // 创建时间 —— 应用生成(insert 时赋值,见数据库设计 6.5 节)
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
