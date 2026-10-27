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

// 盘子:平台公告 —— 对应表 t_announcement。
//
// 单向广播:管理员写、所有人看,不与任何其它表联动;仅 status 两态(0 草稿仅管理员可见 / 1 已发布门户可见),无流转规则,可随时 0↔1 上下线。
// publish_time 是点"发布"的时刻,与 created_at(写好时)可能隔好几天,故两者都得存。
@Data
@Entity
@Table(name = "t_announcement")
public class Announcement {

    // 主键,数据库自增
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 发布人(管理员),关联 t_user.id
    @Column(name = "publisher_id", nullable = false)
    private Long publisherId;

    // 公告标题
    @Column(name = "title", length = 200, nullable = false)
    private String title;

    // 公告正文。
    // ⚠️ 长文本三件套缺一不可:@Comment 保列注释、columnDefinition = "text" 定列类型、length = 65535 供 Hibernate 比对;漏了 length 会每次启动重复 alter table(D-21)。
    @Comment("公告正文")
    @Column(name = "content", columnDefinition = "text", length = 65535)
    private String content;

    // 公告状态:1 已发布 / 0 草稿
    @Comment("1 已发布 / 0 草稿")
    @Column(name = "status", nullable = false, columnDefinition = "tinyint")
    private Integer status;

    // 发布时间 —— 真正对外可见的时刻;草稿状态为空
    @Column(name = "publish_time")
    private LocalDateTime publishTime;

    // 记录创建时间 —— 应用生成(insert 时赋值,见数据库设计 6.5 节)
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
