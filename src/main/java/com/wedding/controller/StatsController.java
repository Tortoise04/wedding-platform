package com.wedding.controller;

import com.wedding.annotation.RequireAdmin;
import com.wedding.annotation.RequireMerchant;
import com.wedding.common.Result;
import com.wedding.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// 服务员:数据统计(商家端 /api/merchant/stats、管理端 /api/admin/stats)。
@RestController
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    // 商家端:本店铺经营数据
    @RequireMerchant
    @GetMapping("/api/merchant/stats")
    public Result<Map<String, Object>> merchantStats(@RequestAttribute("userId") Long userId) {
        return Result.success(statsService.merchantStats(userId));
    }

    // 管理端:平台整体数据
    @RequireAdmin
    @GetMapping("/api/admin/stats")
    public Result<Map<String, Object>> platformStats() {
        return Result.success(statsService.platformStats());
    }
}
