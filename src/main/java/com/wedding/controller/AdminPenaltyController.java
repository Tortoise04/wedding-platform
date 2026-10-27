package com.wedding.controller;

import com.wedding.annotation.RequireAdmin;
import com.wedding.common.Result;
import com.wedding.dto.CreatePenaltyRequest;
import com.wedding.entity.Penalty;
import com.wedding.enums.PenaltyType;
import com.wedding.service.PenaltyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 服务员:管理端 · 违规处罚(需求 FR-A-01-4 / FR-A-03-5)。
//
// 查全站处罚、看某商家处罚历史、执行处罚(警告 / 服务下架 / 封禁)并落地到服务或店铺。
// ⚠️ 路径挂 /api/admin/**,权限一律 @RequireAdmin;与商家自助上下架的 /api/merchant/services/{id}/shelf 不同。
@RestController
@RequestMapping("/api/admin/penalties")
@RequiredArgsConstructor
public class AdminPenaltyController {

    private final PenaltyService penaltyService;

    // 全站处罚记录 —— merchantId / penaltyType / status 全部可选,都不传就是全部,按处罚时间倒序
    @RequireAdmin
    @GetMapping
    public Result<List<Penalty>> list(
            @RequestParam(required = false) Long merchantId,
            @RequestParam(required = false) PenaltyType penaltyType,
            @RequestParam(required = false) Integer status) {
        return Result.success(penaltyService.listAllPenalties(merchantId, penaltyType, status));
    }

    // 某商家的处罚历史(管理端)—— 越权查看该商家的全部罚单,按处罚时间倒序
    @RequireAdmin
    @GetMapping("/merchant/{merchantId}")
    public Result<List<Penalty>> byMerchant(@PathVariable Long merchantId) {
        return Result.success(penaltyService.listByMerchant(merchantId));
    }

    // 执行处罚(管理员),执行人从 token 取,不放在请求体里
    @RequireAdmin
    @PostMapping
    public Result<Penalty> execute(@RequestBody CreatePenaltyRequest request,
                                   @RequestAttribute("userId") Long adminId) {
        return Result.success(penaltyService.executePenalty(adminId, request));
    }
}
