package com.wedding.controller;

import com.wedding.annotation.RequireLogin;
import com.wedding.common.Result;
import com.wedding.dto.MerchantApplyRequest;
import com.wedding.entity.Merchant;
import com.wedding.service.MerchantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

// 服务员:商家入驻(用户端,路径 /api/merchant,均 @RequireLogin)。
@RestController
@RequestMapping("/api/merchant")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    // 提交商家入驻申请(待管理员审核,通过后账号 role 才变 MERCHANT)
    @RequireLogin
    @PostMapping("/apply")
    public Result<Merchant> apply(@RequestBody MerchantApplyRequest request,
                                  @RequestAttribute("userId") Long userId) {
        return Result.success(merchantService.apply(userId, request));
    }

    // 查看我的入驻申请与审核进度(未提交过则报错,前端据此判断能否再申请)
    @RequireLogin
    @GetMapping("/my")
    public Result<Merchant> my(@RequestAttribute("userId") Long userId) {
        return Result.success(merchantService.getMyApplication(userId));
    }
}