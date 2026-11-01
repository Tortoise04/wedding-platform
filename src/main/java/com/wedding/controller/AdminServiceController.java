package com.wedding.controller;

import com.wedding.annotation.RequireAdmin;
import com.wedding.common.Result;
import com.wedding.entity.WeddingService;
import com.wedding.service.ServiceManageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 服务员:管理端 · 服务内容审核(需求 FR-A-02-1)。
//
// 商家发布的服务先落在 PENDING,必须经管理员审核通过(APPROVED)才可能被用户看到。
@RestController
@RequestMapping("/api/admin/services")
@RequiredArgsConstructor
public class AdminServiceController {

    private final ServiceManageService serviceManageService;

    // 服务列表:按内容审核状态 / 商家筛选(都不传 = 全部)
    @RequireAdmin
    @GetMapping
    public Result<List<WeddingService>> list(@RequestParam(required = false) String auditStatus,
                                             @RequestParam(required = false) Long merchantId) {
        return Result.success(serviceManageService.listServicesForAdmin(auditStatus, merchantId));
    }

    // 审核服务:approved=true → APPROVED;false → REJECTED
    @RequireAdmin
    @PostMapping("/{id}/audit")
    public Result<WeddingService> audit(@PathVariable Long id,
                                        @RequestParam Boolean approved,
                                        @RequestAttribute("userId") Long adminId) {
        return Result.success(serviceManageService.auditService(adminId, id, approved));
    }

    // 管理端上下架:管理员在「商家详情抽屉」的商品页签直接给商家的服务上下架
    @RequireAdmin
    @PostMapping("/{id}/shelf")
    public Result<WeddingService> toggleShelf(@PathVariable Long id,
                                              @RequestParam Integer shelfStatus) {
        return Result.success(serviceManageService.adminToggleShelf(id, shelfStatus));
    }
}
