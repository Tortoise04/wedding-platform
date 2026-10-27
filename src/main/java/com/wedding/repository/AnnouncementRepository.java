package com.wedding.repository;

import com.wedding.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

// 冷库:平台公告的存取。
//
// 额外继承 JpaSpecificationExecutor,管理端按状态动态筛选时用来拼条件。
public interface AnnouncementRepository extends JpaRepository<Announcement, Long>,
        JpaSpecificationExecutor<Announcement> {

    // 门户首页用:按状态取公告,发布时间倒序。
    //
    // ⚠️ 末尾多一个 IdDesc:publish_time 是秒级 DATETIME,同秒发布的多条只按时间排序不稳定,
    // 再挂主键降序当定序器,同秒也恒为"后建的在前"(曾致罚单列表同秒乱序)。
    List<Announcement> findByStatusOrderByPublishTimeDescIdDesc(Integer status);
}
