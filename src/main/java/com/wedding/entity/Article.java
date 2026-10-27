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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

// 盘子:婚庆资讯 —— 对应表 t_article。
//
// 结构是公告的加强版(标题 / 正文 / 草稿与发布 / 发布人),多出封面图、作者、阅读量三样展示字段;其中 view_count 每被门户详情页点开一次 +1,不写成"查出来 +1 再存回",得交给数据库原子自增(见 ArticleRepository)。
// 另有 updated_at:资讯长、改错别字/补图是常态,最后修改时间要单独记,公告短则无此字段。
@Data
@Entity
@Table(name = "t_article")
public class Article {

    // 主键,数据库自增
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 发布人(管理员),关联 t_user.id
    @Column(name = "publisher_id", nullable = false)
    private Long publisherId;

    // 资讯标题
    @Column(name = "title", length = 200, nullable = false)
    private String title;

    // 封面图路径
    @Column(name = "cover_url", length = 255)
    private String coverUrl;

    // 正文 —— 长文本,与 Announcement.content 同样的三件套(length 不能漏,见 D-21)
    @Comment("正文")
    @Column(name = "content", columnDefinition = "text", length = 65535)
    private String content;

    // 作者 / 来源
    @Column(name = "author", length = 50)
    private String author;

    // 阅读量 —— 每被门户详情页点开一次 +1;INT 不是 TINYINT,【不需要】columnDefinition = "tinyint"(D-20 只针对 tinyint)
    @Comment("阅读量")
    @Column(name = "view_count", nullable = false)
    private Integer viewCount;

    // 资讯状态:1 已发布 / 0 草稿
    @Comment("1 已发布 / 0 草稿")
    @Column(name = "status", nullable = false, columnDefinition = "tinyint")
    private Integer status;

    // 发布时间 —— 草稿状态为空
    @Column(name = "publish_time")
    private LocalDateTime publishTime;

    // 记录创建时间 —— 应用生成
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 最近修改时间 —— 应用生成(每次 update 自动刷新)
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
