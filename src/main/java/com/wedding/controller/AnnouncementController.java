package com.wedding.controller;

import com.wedding.common.Result;
import com.wedding.entity.Announcement;
import com.wedding.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 服务员:门户端 · 平台公告(需求 FR-A-04)。
//
// ⚠️ 全类不贴权限注解是故意的:公告是给所有人看的广播,按"门户接口不贴注解 = 游客可访问"放行。
// 写公告在 AdminAnnouncementController(/api/admin/announcements),一读一写、权限天差地别,故分文件。
@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    // 已发布公告列表(游客可访问)
    @GetMapping
    public Result<List<Announcement>> list() {
        return Result.success(announcementService.listPublished());
    }

    // 公告详情(游客可访问;未发布的点不进来)
    @GetMapping("/{id}")
    public Result<Announcement> detail(@PathVariable Long id) {
        return Result.success(announcementService.getPublishedDetail(id));
    }
}
