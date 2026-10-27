package com.wedding.repository;

import com.wedding.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

// 冷库:婚庆资讯的存取。
//
// 前两个是派生查询,最后一个是本轮唯一的新东西 —— 一条由数据库自己完成自增的修改语句。
public interface ArticleRepository extends JpaRepository<Article, Long>,
        JpaSpecificationExecutor<Article> {

    // 门户列表:已发布资讯,按发布时间倒序(同秒用 id 兜底,理由同 AnnouncementRepository)
    List<Article> findByStatusOrderByPublishTimeDescIdDesc(Integer status);

    // 门户"热门资讯":已发布资讯,按阅读量倒序
    List<Article> findByStatusOrderByViewCountDescIdDesc(Integer status);

    // 阅读量 +1(★ 本轮核心):把"加"交给数据库,规避读-改-写丢更新。
    //
    // @Modifying 声明这是 update(不加会被当查询执行);clearAutomatically 清一级缓存,后续 findById 才回库取新值。
    // where 带 status = 1 顺手验"存在 + 已发布",影响行数为 0 即拒。
    @Modifying(clearAutomatically = true)
    @Query("update Article a set a.viewCount = a.viewCount + 1 where a.id = :id and a.status = 1")
    int increaseViewCount(@Param("id") Long id);
}
