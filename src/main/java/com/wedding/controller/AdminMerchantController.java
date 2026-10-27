package com.wedding.controller;

import com.wedding.annotation.RequireAdmin;
import com.wedding.common.Result;
import com.wedding.dto.MerchantAuditRequest;
import com.wedding.entity.Merchant;
import com.wedding.enums.AuditStatus;
import com.wedding.service.MerchantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 服务员:管理端 · 商家入驻审核(需求 FR-A-01-1)。
//
// ⚠️ 别把"服务审核"写进本类(那是 AdminServiceController 的事):同一 Controller 内重复的 @PostMapping 路径会让 Spring 启动报 Ambiguous mapping。
@RestController
@RequestMapping("/api/admin/merchants")
@RequiredArgsConstructor
public class AdminMerchantController {

    private final MerchantService merchantService;

    // 商家列表:按审核状态筛选 + 关键字检索(店铺名/联系电话),不传 = 全部
    @RequireAdmin
    @GetMapping
    public Result<List<Merchant>> list(@RequestParam(required = false) AuditStatus auditStatus,
                                       @RequestParam(required = false) String keyword) {
        return Result.success(merchantService.listMerchantsForAdmin(auditStatus, keyword));
    }

    // 审核商家入驻申请:通过 → APPROVED,并把账号 role 改成 MERCHANT
    @RequireAdmin
    @PostMapping("/{id}/audit")
    public Result<Merchant> audit(@PathVariable Long id,
                                  @RequestBody MerchantAuditRequest request) {
        return Result.success(merchantService.audit(id, request));
    }
}
