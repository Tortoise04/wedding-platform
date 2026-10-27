package com.wedding.controller;

import com.wedding.annotation.RequireMerchant;
import com.wedding.common.Result;
import com.wedding.dto.UpdateMerchantRequest;
import com.wedding.entity.Merchant;
import com.wedding.service.MerchantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 服务员:商家门户(路径 /api/merchants,复数)。
//
// 两个公开接口给游客浏览商家(FR-U-02),一个商家接口给本人维护店铺资料(FR-M-01-5)。
// ⚠️ 与 /api/merchant(单数,入驻申请/进度)区分:这里是"商家资源集合"的对外展示。
@RestController
@RequestMapping("/api/merchants")
@RequiredArgsConstructor
public class MerchantPortalController {

    private final MerchantService merchantService;

    // 商家列表(公开:已过审且启用的商家)
    @GetMapping
    public Result<List<Merchant>> list() {
        return Result.success(merchantService.listApprovedMerchants());
    }

    // 商家详情(公开:未过审/已停用的返回 400 不泄露)
    @GetMapping("/{id}")
    public Result<Merchant> detail(@PathVariable Long id) {
        return Result.success(merchantService.getMerchantById(id));
    }

    // 维护本人店铺信息(商家:身份由令牌推导,只能改自己的店)
    @RequireMerchant
    @PutMapping("/profile")
    public Result<Merchant> updateProfile(@RequestBody UpdateMerchantRequest request,
                                          @RequestAttribute("userId") Long userId) {
        return Result.success(merchantService.updateShopProfile(userId, request));
    }
}
