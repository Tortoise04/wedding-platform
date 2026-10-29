package com.wedding.service;

import com.wedding.dto.AdminCancelOrderRequest;
import com.wedding.dto.AfterSaleRequest;
import com.wedding.dto.CreateOrderRequest;
import com.wedding.entity.OrderStatusLog;
import com.wedding.entity.WeddingOrder;
import com.wedding.enums.OrderStatus;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;


// 厨师:订单这道菜 —— 用户下单/取消,商家接单/拒单/推进,管理端监管。
public interface OrderService {
    // 用户下单(生成 PENDING 待审核订单)
    WeddingOrder createOrder(CreateOrderRequest request, Long userId);
    // 商家接单(PENDING → CONFIRMED)
    WeddingOrder confirmOrder(Long orderId, Long userId);
    // 商家拒单(PENDING → CANCELLED,reason 必填,原因写进流水 remark)
    WeddingOrder rejectOrder(Long orderId, Long userId, String reason);
    // 商家推进履约(CONFIRMED → IN_PROGRESS → COMPLETED,remark 可选,写进流水)
    WeddingOrder progressOrder(Long orderId, Long userId, String remark);
    // 商家登记售后处理(只写售后三字段,不动订单状态机;仅 COMPLETED 订单可登记)
    WeddingOrder handleAfterSale(Long orderId, Long userId, AfterSaleRequest request);
    // 用户取消本人订单(PENDING → CANCELLED,reason 可选)
    WeddingOrder cancelOrder(Long orderId, Long userId, String reason);

    // 用户:我的订单列表(状态可选),按下单时间倒序,分页
    Page<WeddingOrder> listMyOrders(Long userId, OrderStatus status, int page, int size);

    // 用户:订单详情(含状态流转记录),只能看自己的
    Map<String, Object> getOrderDetail(Long userId, Long orderId);

    // 商家:本店铺订单列表(状态可选),按下单时间倒序,分页
    Page<WeddingOrder> listMerchantOrders(Long userId, OrderStatus status, int page, int size);

    // 商家:本店铺某订单的履约轨迹(只能看自己店的;含拒单原因、履约备注)
    List<OrderStatusLog> listMerchantOrderLogs(Long orderId, Long userId);

    // ===== 管理端 · 内容与订单监管(FR-A-02) =====

    // 全站订单查看(FR-A-02-2):状态 / 商家 / 用户 / 时间区间任意组合筛选(都为空 = 全部),按下单时间倒序,分页
    Page<WeddingOrder> listAllOrders(OrderStatus status, Long merchantId, Long userId,
                                     LocalDate startDate, LocalDate endDate, int page, int size);

    // 履约监督(FR-A-02-3):某订单的完整流转轨迹(按时间正序)
    List<OrderStatusLog> listOrderLogs(Long orderId);

    // 履约监督:识别滞留单 —— PENDING 超 pendingHours 小时未接单 / CONFIRMED 超 confirmedDays 天未推进
    List<WeddingOrder> listAbnormalOrders(int pendingHours, int confirmedDays);

    // 异常处置:管理员强制取消订单(写 ADMIN 流水)
    WeddingOrder adminCancelOrder(Long orderId, Long adminId, AdminCancelOrderRequest request);
}
