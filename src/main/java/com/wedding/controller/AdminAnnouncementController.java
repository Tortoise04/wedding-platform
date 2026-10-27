package com.wedding.controller;

import com.wedding.annotation.RequireAdmin;
import com.wedding.common.Result;
import com.wedding.dto.CreateAnnouncementRequest;
import com.wedding.entity.Announcement;
import com.wedding.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 服务员:管理端 · 公告维护(需求 FR-A-04)。
//
// 列表(含草稿) / 发布或存草稿 / 上线下线。
// ⚠️ 全部 @RequireAdmin;发布人 adminId 从 token 取,请求体里没有这个字段。
@RestController
@RequestMapping("/api/admin/announcements")
@RequiredArgsConstructor
public class AdminAnnouncementController {

    private final AnnouncementService announcementService;

    // 公告列表:status 不传 = 草稿和已发布全要
    @RequireAdmin
    @GetMapping
    public Result<List<Announcement>> list(@RequestParam(required = false) Integer status) {
        return Result.success(announcementService.listForAdmin(status));
    }

    // 发布公告(或存草稿):发布人从 token 取
    @RequireAdmin
    @PostMapping
    public Result<Announcement> publish(@RequestBody CreateAnnouncementRequest request,
                                        @RequestAttribute("userId") Long adminId) {
        return Result.success(announcementService.publishAnnouncement(adminId, request));
    }

    // 上线(status=1)/ 下线(status=0)
    @RequireAdmin
    @PutMapping("/{id}/status")
    public Result<Announcement> changeStatus(@PathVariable Long id,
                                             @RequestParam Integer status) {
        return Result.success(announcementService.changeStatus(id, status));
    }
}
