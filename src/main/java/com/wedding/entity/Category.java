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

// 盘子:分类(套餐/场地/司仪/摄影/婚纱/婚车) —— 对应表 t_category。
//
// 主键用 Long(不是 long):插库前为 null,由数据库自增。
// ⚠️ 字段与建表脚本一一对齐:name 长 50、sort_order/status 非空;created_at 由 @CreationTimestamp 在 insert 时赋值,不能写 insertable = false,否则插完读不回值(D-17)。
@Data
@Entity
@Table(name = "t_category")
public class Category {

    // 主键,数据库自增
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 分类名称,唯一
    @Column(name = "name", length = 50, nullable = false)
    private String name;

    // 展示排序,数值越小越靠前(默认 0,与建表脚本一致)
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    // 1 启用 / 0 停用(默认 1,与建表脚本一致)
    @Comment("1 启用 / 0 停用")
    @Column(name = "status", nullable = false, columnDefinition = "tinyint")
    private Integer status = 1;

    // 创建时间 —— 应用生成(insert 时赋值);创建后不再改动,故 updatable = false
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
