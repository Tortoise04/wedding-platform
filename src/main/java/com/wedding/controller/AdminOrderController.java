package com.wedding.controller;

import com.wedding.annotation.RequireAdmin;
import com.wedding.common.PageResult;
import com.wedding.common.Result;
import com.wedding.dto.AdminCancelOrderRequest;
import com.wedding.entity.OrderStatusLog;
import com.wedding.entity.WeddingOrder;
import com.wedding.enums.OrderStatus;
import com.wedding.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

// 服务员:管理端 · 内容与订单监管(需求 FR-A-02)。
//
// 全站订单查看(FR-A-02-2)+ 履约监督/滞留单(FR-A-02-3)+ 强制取消订单。
// ⚠️ 路径挂 /api/admin/**,与 /api/orders、/api/merchant/orders 分开;权限一律 @RequireAdmin。
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    // 全站订单列表(管理端)—— 状态 / 商家 / 用户 / 时间区间任意组合筛选,分页,返回 {list,total,...}
    //   status / merchantId / userId / startDate / endDate 全部可选,都不传即全站;按下单时间倒序。
    @RequireAdmin
    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Long merchantId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(PageResult.of(
                orderService.listAllOrders(status, merchantId, userId, startDate, endDate, page, size)));
    }

    // 履约监督 · 滞留单(异常订单)—— 挂着不动的订单:
    //   PENDING 超过 pendingHours 小时仍未接单;CONFIRMED 超过 confirmedDays 天仍未推进履约。
    // 两个阈值都给默认值,不传就用默认(48 小时 / 7 天)。
    @RequireAdmin
    @GetMapping("/abnormal")
    public Result<List<WeddingOrder>> abnormal(
            @RequestParam(required = false, defaultValue = "48") int pendingHours,
            @RequestParam(required = false, defaultValue = "7") int confirmedDays) {
        return Result.success(orderService.listAbnormalOrders(pendingHours, confirmedDays));
    }

    // 履约监督 · 流转轨迹 —— 某订单从下单至今的每一步(谁、在什么时候、把状态从哪改到哪)
    @RequireAdmin
    @GetMapping("/{id}/logs")
    public Result<List<OrderStatusLog>> logs(@PathVariable Long id) {
        return Result.success(orderService.listOrderLogs(id));
    }

    // 异常处置 · 强制取消订单(管理员介入),原因必填,留 ADMIN 流水
    @RequireAdmin
    @PostMapping("/{id}/force-cancel")
    public Result<WeddingOrder> forceCancel(@PathVariable Long id,
                                            @RequestBody AdminCancelOrderRequest request,
                                            @RequestAttribute("userId") Long adminId) {
        return Result.success(orderService.adminCancelOrder(id, adminId, request));
    }
}
