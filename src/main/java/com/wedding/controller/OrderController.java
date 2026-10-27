package com.wedding.controller;

import com.wedding.annotation.RequireLogin;
import com.wedding.common.PageResult;
import com.wedding.common.Result;
import com.wedding.dto.CreateOrderRequest;
import com.wedding.entity.WeddingOrder;
import com.wedding.enums.OrderStatus;
import com.wedding.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// 服务员:用户端 · 订单(路径 /api/orders,均 @RequireLogin,只能操作本人订单)。
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    // 我的订单列表(状态可选,不传 = 全部;分页,返回 {list,total,...})
    @RequireLogin
    @GetMapping
    public Result<Map<String, Object>> list(@RequestParam(required = false) OrderStatus status,
                                            @RequestAttribute("userId") Long userId,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        return Result.success(PageResult.of(orderService.listMyOrders(userId, status, page, size)));
    }

    // 订单详情(含状态流转记录)
    @RequireLogin
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id,
                                              @RequestAttribute("userId") Long userId) {
        return Result.success(orderService.getOrderDetail(userId, id));
    }

    // 用户下单(生成 PENDING 待审核订单)
    @RequireLogin
    @PostMapping
    public Result<WeddingOrder> create(@RequestBody CreateOrderRequest request,
                                       @RequestAttribute("userId") Long userId) {
        return Result.success(orderService.createOrder(request, userId));
    }
    // 用户取消本人订单(PENDING → CANCELLED,reason 可选)
    @RequireLogin
    @PostMapping("/{id}/cancel")
    public Result<WeddingOrder> cancel(@PathVariable Long id,
                                       @RequestParam(required = false) String reason,
                                       @RequestAttribute("userId") Long userId) {
        return Result.success(orderService.cancelOrder(id, userId, reason));
    }
}