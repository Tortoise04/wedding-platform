package com.wedding.service;

import com.wedding.dto.CreateAnnouncementRequest;
import com.wedding.entity.Announcement;

import java.util.List;

// 厨师:平台公告这道菜。
//
// ★ 同一份数据两副面孔:管理端 listForAdmin 连草稿一起看,门户 listPublished 只给 status = 1。
// "哪些能给外人看"一律在服务端判断 —— 前端能拿到的就等于已经泄露,前端过滤是纸糊的门。
public interface AnnouncementService {

    // 【管理端】发布公告:adminId 来自 token,status 决定"直接发布"还是"存草稿"
    Announcement publishAnnouncement(Long adminId, CreateAnnouncementRequest request);

    // 【管理端】公告列表:status 传 null 就是"草稿+已发布"全要
    List<Announcement> listForAdmin(Integer status);

    // 【门户】已发布公告列表 —— 不带参数,因为"给外人看的"永远只有一种:status = 1
    List<Announcement> listPublished();

    // 【门户】查看某条公告详情
    Announcement getPublishedDetail(Long id);

    // 【管理端】上线 / 下线:status = 1 上线(顺便补发布时间)、0 下线
    Announcement changeStatus(Long id, Integer status);
}
