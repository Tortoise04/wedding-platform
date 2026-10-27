package com.wedding.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

// 盘子:一个"用户"长什么样。
// 这个类 = 数据库里的 t_user 表,字段 = 表的列。
// 注意:表名用 t_ 前缀,因为 user/order 等是数据库保留字,直接用会报错。
@Data
@Entity
@Table(name = "t_user")
public class User {

    // 主键,数据库自增
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 登录名,全局唯一
    @Column(name = "username", length = 50, nullable = false)
    private String username;

    // 密码,只写入不返回(WRITE_ONLY)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "password", length = 100, nullable = false)
    private String password;

    // 真实姓名
    @Column(name = "real_name", length = 50)
    private String realName;

    // 手机号
    @Column(name = "phone", length = 20)
    private String phone;

    // 邮箱
    @Column(name = "email", length = 100)
    private String email;

    // 头像文件路径
    @Column(name = "avatar", length = 255)
    private String avatar;

    // 角色:USER(普通用户) / MERCHANT(商家) / ADMIN(管理员)
    @Column(name = "role", length = 20, nullable = false)
    private String role;

    // 账号状态:1 正常 / 0 禁用
    @Comment("账号状态：1 正常 / 0 禁用")
    @Column(name = "status", nullable = false, columnDefinition = "tinyint")
    private Integer status;

    // 注册时间 —— 应用生成(insert 时赋值,见数据库设计 6.5 节)
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 最近修改时间 —— 应用生成(insert / update 时赋值,见数据库设计 6.5 节)
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
