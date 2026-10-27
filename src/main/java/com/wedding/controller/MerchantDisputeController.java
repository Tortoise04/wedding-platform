package com.wedding.controller;

import com.wedding.annotation.RequireMerchant;
import com.wedding.common.Result;
import com.wedding.entity.Dispute;
import com.wedding.service.DisputeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 服务员:商家端 · 针对本店的争议(路径 /api/merchant/disputes,均 @RequireMerchant,只读)。
@RestController
@RequestMapping("/api/merchant/disputes")
@RequiredArgsConstructor
public class MerchantDisputeController {
    private final DisputeService disputeService;

    // 本店被投诉的争议列表(按发起时间倒序,直接返回争议实体,与用户端口径一致)
    @RequireMerchant
    @GetMapping
    public Result<List<Dispute>> list(@RequestAttribute("userId") Long userId) {
        return Result.success(disputeService.listShopDisputes(userId));
    }
}
