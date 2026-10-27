package com.wedding.controller;

import com.wedding.annotation.RequireMerchant;
import com.wedding.common.PageResult;
import com.wedding.common.Result;
import com.wedding.dto.AfterSaleRequest;
import com.wedding.entity.WeddingOrder;
import com.wedding.enums.OrderStatus;
import com.wedding.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// 服务员:商家端 · 订单履约(路径 /api/merchant/orders,均 @RequireMerchant,只能操作本店订单)。
@RestController
@RequestMapping("/api/merchant/orders")
@RequiredArgsConstructor
public class MerchantOrderController {
    private final OrderService orderService;

    // 本店铺订单列表(状态可选,不传 = 全部;分页,返回 {list,total,...})
    @RequireMerchant
    @GetMapping
    public Result<Map<String, Object>> list(@RequestParam(required = false) OrderStatus status,
                                            @RequestAttribute("userId") Long userId,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        return Result.success(PageResult.of(orderService.listMerchantOrders(userId, status, page, size)));
    }

    // 商家接单(PENDING → CONFIRMED)
    @RequireMerchant
    @PostMapping("/{id}/confirm")
    public Result<WeddingOrder> confirm(@PathVariable Long id,
                                        @RequestAttribute("userId") Long userId) {
        return Result.success(orderService.confirmOrder(id, userId));
    }

    // 商家拒单(PENDING → CANCELLED,reason 可选)
    @RequireMerchant
    @PostMapping("/{id}/reject")
    public Result<WeddingOrder> reject(@PathVariable Long id,
                                       @RequestParam(required = false) String reason,
                                       @RequestAttribute("userId") Long userId) {
        return Result.success(orderService.rejectOrder(id, userId, reason));
    }
    // 商家推进履约(CONFIRMED → IN_PROGRESS → COMPLETED,每次推进一档)
    @RequireMerchant
    @PostMapping("/{id}/progress")
    public Result<WeddingOrder> progress(@PathVariable Long id,
                                         @RequestAttribute("userId") Long userId) {
        return Result.success(orderService.progressOrder(id, userId));
    }

    // 商家登记售后处理(只写售后三字段,不改变订单状态机;仅 COMPLETED 订单可登记)
    @RequireMerchant
    @PostMapping("/{id}/after-sale")
    public Result<WeddingOrder> afterSale(@PathVariable Long id,
                                          @RequestBody AfterSaleRequest request,
                                          @RequestAttribute("userId") Long userId) {
        return Result.success(orderService.handleAfterSale(id, userId, request));
    }
}