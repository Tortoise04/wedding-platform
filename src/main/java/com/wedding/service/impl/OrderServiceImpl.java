package com.wedding.service.impl;

import com.wedding.dto.AdminCancelOrderRequest;
import com.wedding.dto.AfterSaleRequest;
import com.wedding.dto.CreateOrderRequest;
import com.wedding.entity.Merchant;
import com.wedding.entity.OrderStatusLog;
import com.wedding.entity.ServiceSchedule;
import com.wedding.entity.WeddingOrder;
import com.wedding.entity.WeddingService;
import com.wedding.enums.OrderStatus;
import com.wedding.exception.BusinessException;
import com.wedding.repository.OrderStatusLogRepository;
import com.wedding.repository.ServiceScheduleRepository;
import com.wedding.repository.WeddingOrderRepository;
import com.wedding.repository.WeddingServiceRepository;
import com.wedding.service.MerchantService;
import com.wedding.service.OrderService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 厨师:订单这道菜,下单 / 接单 / 拒单都在这里。
//
// 状态机两条铁律:1. 每个流转方法开头先做前置校验,状态不对就抛异常拦下;
// 2. 改状态 + 写流水必须同处一个 @Transactional(要么都成功,要么都回滚)。
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    // 并发防重分两层,缺一不可:
    // 1. 数据库行锁 (findByIdForUpdate) —— 真正负责跨请求/跨实例的串行。@Transactional 在方法返回后才提交,
    //    进程内锁挡不住"后到请求读不到前一笔未提交行"的 check-then-act (测试记录 D-32 ~ D-34)。
    // 2. 进程内锁 —— 同一实例内的快速串行,避免连点在同一事务窗口里重复进入,顺带省掉一次无谓的行锁等待。
    private static final java.util.concurrent.ConcurrentHashMap<String, Object> ORDER_LOCKS = new java.util.concurrent.ConcurrentHashMap<>();

    private final WeddingServiceRepository weddingServiceRepository;
    private final WeddingOrderRepository weddingOrderRepository;
    private final OrderStatusLogRepository orderStatusLogRepository;
    private final ServiceScheduleRepository serviceScheduleRepository;
    private final MerchantService merchantService;

    // 占住档期的订单状态:这三态说明"这一天已经被这单占着",其余(取消 / 拒单 / 已完成)不占。
    private static final List<OrderStatus> OCCUPYING_STATUS =
            List.of(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.IN_PROGRESS);

    // 用户下单:建一条待审核(PENDING)订单并写首条流水。
    //
    // 两道闸门:同一用户对同一服务已有 PENDING 单就拦(幂等);
    // 服务必须已上架(shelf_status = 1)且审核通过(APPROVED)才能预约。
    @Transactional
    @Override
    public WeddingOrder createOrder(CreateOrderRequest request, Long userId) {

        // 服务 id 必填:缺字段时 findById(null) 抛底层异常,最终变成 500
        if (request.getServiceId() == null) {
            throw new BusinessException("请选择要预约的服务");
        }
        WeddingService service = weddingServiceRepository.findByIdForUpdate(request.getServiceId())
                .orElseThrow(() -> new BusinessException("服务不存在: " + request.getServiceId()));
        // 服务必须已上架 + 审核通过才能下单
        if (service.getShelfStatus() == null || service.getShelfStatus() != 1) {
            throw new BusinessException("该服务已下架，无法预约");
        }
        if (!"APPROVED".equals(service.getAuditStatus())) {
            throw new BusinessException("该服务尚未通过审核，无法预约");
        }
        Integer quantity = request.getQuantity();
        if (quantity == null || quantity <= 0) {
            throw new BusinessException("数量不合法");
        }
        if (quantity > 99) {
            throw new BusinessException("数量需在 1–99 之间");
        }
        // 表单必填校验:前端 el-form 已拦一次,这里再拦一次防止直接调接口绕过
        if (request.getWeddingDate() == null) {
            throw new BusinessException("请选择婚礼日期");
        }
        if (request.getWeddingDate().isBefore(LocalDate.now())) {
            throw new BusinessException("婚礼日期不能早于今天");
        }
        if (request.getGuestCount() == null || request.getGuestCount() < 1 || request.getGuestCount() > 2000) {
            throw new BusinessException("宾客人数需在 1–2000 之间");
        }
        if (request.getBudgetMin() != null && request.getBudgetMin().signum() < 0
                || request.getBudgetMax() != null && request.getBudgetMax().signum() < 0) {
            throw new BusinessException("预算不能为负数");
        }
        if (request.getBudgetMin() != null && request.getBudgetMax() != null
                && request.getBudgetMin().compareTo(request.getBudgetMax()) > 0) {
            throw new BusinessException("预算下限不能高于上限");
        }
        if (request.getRemark() != null && request.getRemark().length() > 200) {
            throw new BusinessException("备注不能超过 200 字");
        }
        BigDecimal amount = service.getPrice().multiply(BigDecimal.valueOf(quantity));

        // 幂等检查与落库收在同一临界区;跨事务的串行由上面的服务行锁保证,这里只兜同实例连点
        Object lock = ORDER_LOCKS.computeIfAbsent(userId + ":" + request.getServiceId(), k -> new Object());
        synchronized (lock) {
            if (weddingOrderRepository.existsByUserIdAndServiceIdAndStatus(userId, request.getServiceId(), OrderStatus.PENDING)) {
                throw new BusinessException("您已对该服务提交过订单，请勿重复下单");
            }

            // ★ 档期互斥:同一服务同一天只能有一单占着。必须留在临界区里,
            // 否则两个并发请求会在"查无人占"和"插新单"之间同时通过。
            if (weddingOrderRepository.existsByServiceIdAndWeddingDateAndStatusIn(request.getServiceId(),
                    request.getWeddingDate(), OCCUPYING_STATUS)) {
                throw new BusinessException("该日期已被预约，请选择其他日期");
            }

            // 档期表里有这一天就把状态翻成 0(已占用),让门户的档期列表立刻不再可选。
            // 没设档期的服务不拦 —— 有的服务按天报价但不维护档期表,强制要求档期会把它们全挡死。
            serviceScheduleRepository
                    .findByServiceIdAndScheduleDate(request.getServiceId(), request.getWeddingDate())
                    .filter(s -> s.getStatus() != null && s.getStatus() == 1)
                    .ifPresent(s -> {
                        s.setStatus(0);
                        serviceScheduleRepository.save(s);
                    });

            String orderNo = "O" + System.currentTimeMillis() + (int) (Math.random() * 10000);

            WeddingOrder order = new WeddingOrder();

            order.setOrderNo(orderNo);
            order.setUserId(userId);
            order.setMerchantId(service.getMerchantId());
            order.setServiceId(request.getServiceId());
            order.setWeddingDate(request.getWeddingDate());
            order.setGuestCount(request.getGuestCount());
            order.setBudgetMin(request.getBudgetMin());
            order.setBudgetMax(request.getBudgetMax());
            order.setQuantity(quantity);
            order.setAmount(amount);
            order.setRemark(request.getRemark());
            order.setStatus(OrderStatus.PENDING);
            weddingOrderRepository.save(order);

            writeLog(order.getId(), null, OrderStatus.PENDING, userId, "USER", "用户提交预约订单");
            return order;
        }
    }

    // 商家接单:PENDING → CONFIRMED,记 confirmed_at 并写流水。
    //
    // 两道闸门:归属校验(订单的 merchantId 必须等于本店 shop.getId());
    // 只有 PENDING 能接单,其余状态一律拦下。
    @Transactional
    @Override
    public WeddingOrder confirmOrder(Long orderId, Long userId) {
        WeddingOrder order = weddingOrderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new BusinessException("订单不存在: " + orderId));

        Merchant shop = merchantService.getMyShop(userId);
        if (!order.getMerchantId().equals(shop.getId())) {
            throw new BusinessException("无权操作其他店铺的订单");
        }
        // 前置校验(拦下):只有待审核的订单能接单
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException("订单当前状态不允许接单");
        }

        order.setStatus(OrderStatus.CONFIRMED);
        order.setConfirmedAt(LocalDateTime.now());
        weddingOrderRepository.save(order);

        writeLog(order.getId(), OrderStatus.PENDING, OrderStatus.CONFIRMED, userId, "MERCHANT", "商家接单确认");
        return order;
    }

    // 商家拒单:PENDING → CANCELLED,记拒单原因与时间并写流水。
    // 闸门与接单一致:归属校验 + 只有 PENDING 能拒。
    @Transactional
    @Override
    public WeddingOrder rejectOrder(Long orderId, Long userId, String reason) {
        WeddingOrder order = weddingOrderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new BusinessException("订单不存在: " + orderId));
        Merchant shop = merchantService.getMyShop(userId);
        if (!order.getMerchantId().equals(shop.getId())) {
            throw new BusinessException("无权操作其他店铺的订单");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException("订单当前状态不允许拒绝");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancelReason(reason);
        weddingOrderRepository.save(order);

        writeLog(order.getId(), OrderStatus.PENDING, OrderStatus.CANCELLED, userId, "MERCHANT", "商家拒绝接单");
        // 单子作废,这一天要重新放出来给别人约
        releaseSchedule(order.getServiceId(), order.getWeddingDate());
        return order;
    }

    // 商家履约推进:CONFIRMED → IN_PROGRESS,再推一次 IN_PROGRESS → COMPLETED。
    //
    // ★ 必须先把原状态存进 from 再改 order,否则写流水时取不到"从哪来"。
    // 其余状态(PENDING / CANCELLED / COMPLETED)一律拦下,不允许跳级推进。
    @Transactional
    @Override
    public WeddingOrder progressOrder(Long orderId, Long userId) {
        WeddingOrder order = weddingOrderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new BusinessException("订单不存在: " + orderId));
        Merchant shop = merchantService.getMyShop(userId);
        if (!order.getMerchantId().equals(shop.getId())) {
            throw new BusinessException("无权操作其他店铺的订单");
        }
        OrderStatus from = order.getStatus();          // 先记住"从哪来"
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            order.setStatus(OrderStatus.IN_PROGRESS);  // 开始履约
        } else if (order.getStatus() == OrderStatus.IN_PROGRESS) {
            order.setStatus(OrderStatus.COMPLETED);    // 完成履约
            order.setCompletedAt(LocalDateTime.now());
        } else {
            throw new BusinessException("订单当前状态不允许履约推进");   // 拦下
        }
        weddingOrderRepository.save(order);
        writeLog(order.getId(), from, order.getStatus(), userId, "MERCHANT", "商家履约推进");
        return order;
    }

    // 商家登记售后处理:只写售后三字段(afterSaleStatus / afterSaleNote / afterSaleAt)。
    //
    // ★ 售后不是状态机的一档,所以这里绝不动 order.status,也不写状态流水。
    // ★ 必须 findById 查出原对象再改,禁止 new —— new 出来的是游离态,save 会 INSERT 一条孤儿订单。
    // 五道闸门按顺序拦:入参状态 → 说明非空 → 订单存在 → 订单已完成 → 店铺归属。
    @Transactional
    @Override
    public WeddingOrder handleAfterSale(Long orderId, Long userId, AfterSaleRequest request) {
        if (request.getStatus() == null || (request.getStatus() != 1 && request.getStatus() != 2)) {
            throw new BusinessException("售后状态不合法");
        }
        String note = request.getNote() == null ? "" : request.getNote().trim();
        if (note.isEmpty()) {
            throw new BusinessException("售后处理说明不能为空");
        }
        WeddingOrder order = weddingOrderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new BusinessException("订单不存在: " + orderId));
        // 只有已完成订单才谈得上售后;其余状态(含已取消)一律拦下
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new BusinessException("只有已完成订单可以登记售后");
        }
        // 归属校验:订单的 merchantId 必须等于当前登录人名下的店铺
        Merchant shop = merchantService.getMyShop(userId);
        if (!order.getMerchantId().equals(shop.getId())) {
            throw new BusinessException("无权操作其他店铺的订单");
        }

        order.setAfterSaleStatus(request.getStatus());
        order.setAfterSaleNote(note);
        order.setAfterSaleAt(LocalDateTime.now());
        // 注意:此处不 setStatus,save 只会 UPDATE 售后三字段,订单状态机原样保留
        weddingOrderRepository.save(order);
        return order;
    }

    // 用户取消自己的订单:CANCELLED 落库,记原因与时间并写流水。
    // 两道闸门:只能取消本人的订单(归属校验);且只有 PENDING 能取消。
    @Transactional
    @Override
    public WeddingOrder cancelOrder(Long orderId, Long userId, String reason) {

        // 状态读取已由 findByIdForUpdate 串行化;这里的进程内锁只兜同实例连点,避免重复写状态日志
        Object lock = ORDER_LOCKS.computeIfAbsent("cancel:" + orderId, k -> new Object());
        synchronized (lock) {
            WeddingOrder order = weddingOrderRepository.findByIdForUpdate(orderId)
                    .orElseThrow(() -> new BusinessException("订单不存在: " + orderId));
            // 归属校验:不是本人的订单直接拦
            if (!order.getUserId().equals(userId)) {
                throw new BusinessException("无权操作该订单");
            }

            if (order.getStatus() != OrderStatus.PENDING) {
                throw new BusinessException("订单当前状态不允许取消");
            }

            order.setStatus(OrderStatus.CANCELLED);
            order.setCancelledAt(LocalDateTime.now());
            order.setCancelReason(reason);
            weddingOrderRepository.save(order);

            writeLog(order.getId(), OrderStatus.PENDING, OrderStatus.CANCELLED, userId, "USER", "用户取消订单");
            releaseSchedule(order.getServiceId(), order.getWeddingDate());
            return order;
        }

    }


    // 用户:我的订单列表(状态可选)。userId 写死成"当前用户",只有状态是变量,故用 Specification 拼;分页。
    @Override
    public Page<WeddingOrder> listMyOrders(Long userId, OrderStatus status, int page, int size) {
        Specification<WeddingOrder> spec = (root, query, cb) -> {
            List<Predicate> conditions = new ArrayList<>();
            conditions.add(cb.equal(root.get("userId"), userId));
            if (status != null) {
                conditions.add(cb.equal(root.get("status"), status));
            }
            query.orderBy(cb.desc(root.get("createdAt")), cb.desc(root.get("id")));
            return cb.and(conditions.toArray(new Predicate[0]));
        };
        return weddingOrderRepository.findAll(spec, PageRequest.of(page, size));
    }

    // 用户:订单详情 = 订单主体 + 状态流转记录,只能看自己的(归属校验)。
    @Override
    public Map<String, Object> getOrderDetail(Long userId, Long orderId) {
        WeddingOrder order = weddingOrderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("订单不存在: " + orderId));
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权查看该订单");
        }
        List<OrderStatusLog> logs = orderStatusLogRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("order", order);
        result.put("logs", logs);
        return result;
    }

    // 商家:本店铺订单列表(状态可选)。先解析"我是哪个商家",merchantId 写死、状态作变量;分页。
    @Override
    public Page<WeddingOrder> listMerchantOrders(Long userId, OrderStatus status, int page, int size) {
        Merchant shop = merchantService.getMyShop(userId);
        Specification<WeddingOrder> spec = (root, query, cb) -> {
            List<Predicate> conditions = new ArrayList<>();
            conditions.add(cb.equal(root.get("merchantId"), shop.getId()));
            if (status != null) {
                conditions.add(cb.equal(root.get("status"), status));
            }
            query.orderBy(cb.desc(root.get("createdAt")), cb.desc(root.get("id")));
            return cb.and(conditions.toArray(new Predicate[0]));
        };
        return weddingOrderRepository.findAll(spec, PageRequest.of(page, size));
    }


    // 管理端 · 内容与订单监管(需求 FR-A-02)

    // 全站订单查看(FR-A-02-2)—— 按状态 / 商家 / 用户 / 时间区间任意组合筛选,分页。
    //
    // 筛选项都可传可不传,组合太多写不出固定方法名,故用 Specification
    // 在运行时按需往 where 后拼条件。只读查询,不加 @Transactional。
    @Override
    public Page<WeddingOrder> listAllOrders(OrderStatus status, Long merchantId, Long userId,
                                            LocalDate startDate, LocalDate endDate, int page, int size) {
        Specification<WeddingOrder> spec = (root, query, cb) -> {
            List<Predicate> conditions = new ArrayList<>();
            if (status != null) {
                conditions.add(cb.equal(root.get("status"), status));
            }
            if (merchantId != null) {
                conditions.add(cb.equal(root.get("merchantId"), merchantId));
            }
            if (userId != null) {
                conditions.add(cb.equal(root.get("userId"), userId));
            }
            if (startDate != null) {
                // 含当天 00:00 起
                conditions.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("createdAt"),
                        startDate.atStartOfDay()));
            }
            if (endDate != null) {
                // 含当天一整天:用"次日 00:00 之前"(左闭右开),避免漏掉当天的订单
                conditions.add(cb.lessThan(root.<LocalDateTime>get("createdAt"),
                        endDate.plusDays(1).atStartOfDay()));
            }
            // 无论筛不筛,一律按下单时间倒序(最新的排最前),id 兜底定序
            query.orderBy(cb.desc(root.get("createdAt")), cb.desc(root.get("id")));
            return cb.and(conditions.toArray(new Predicate[0]));
        };
        return weddingOrderRepository.findAll(spec, PageRequest.of(page, size));
    }

    // 履约监督 · 流转轨迹(FR-A-02-3):某订单从下单至今每一步,按时间正序
    @Override
    public List<OrderStatusLog> listOrderLogs(Long orderId) {
        // 先确认订单存在,避免"查一个不存在的订单"返回空列表、让人以为"这单没流过转"
        if (!weddingOrderRepository.existsById(orderId)) {
            throw new BusinessException("订单不存在: " + orderId);
        }
        return orderStatusLogRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
    }

    // 履约监督 · 识别滞留(异常)订单(FR-A-02-3)。
    //
    // "异常" = 卡在某个状态太久:PENDING 超过 pendingHours 小时(商家不接单)、
    // CONFIRMED 超过 confirmedDays 天(接了不推进)。两条规则各查一次再合并返回。
    @Override
    public List<WeddingOrder> listAbnormalOrders(int pendingHours, int confirmedDays) {
        LocalDateTime now = LocalDateTime.now();
        List<WeddingOrder> abnormal = new ArrayList<>();
        abnormal.addAll(weddingOrderRepository
                .findByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(OrderStatus.PENDING,
                        now.minusHours(pendingHours)));
        abnormal.addAll(weddingOrderRepository
                .findByStatusAndConfirmedAtBeforeOrderByConfirmedAtAsc(OrderStatus.CONFIRMED,
                        now.minusDays(confirmedDays)));
        return abnormal;
    }

    // 异常处置 · 管理员强制取消订单。
    //
    // 闸门与用户取消(cancelOrder)不同:管理员可取消任何人的 PENDING / CONFIRMED 单,
    // 但 COMPLETED(钱货两清)与 CANCELLED(别重复取消)不能碰,IN_PROGRESS 要走争议流程。
    @Transactional
    @Override
    public WeddingOrder adminCancelOrder(Long orderId, Long adminId, AdminCancelOrderRequest request) {

        // ① 取理由 + 前置校验(★校验必须放在任何写库动作之前)
        if (request.getReason() == null || request.getReason().isBlank())   {
            throw new BusinessException("取消原因不能为空");
        }

        // ② 查单,查不到即拦
        WeddingOrder order =weddingOrderRepository.findByIdForUpdate(orderId)
                .orElseThrow(()-> new BusinessException("订单不存在: " + orderId));

        // ③ 前置状态校验(★管理员与用户的闸门不一样):
        //    允许 PENDING / CONFIRMED;拦 COMPLETED(钱货两清)、
        //    CANCELLED(别重复取消)、IN_PROGRESS(服务进行中,要停得走争议流程)
        OrderStatus from = order.getStatus();
        switch (order.getStatus()){
            case COMPLETED ->
                    throw new BusinessException("订单已完成,不支持强制取消");
            case CANCELLED ->
                throw new BusinessException("订单已取消,请勿重复取消");
            case IN_PROGRESS ->
                throw new BusinessException("订单履约中,服务已在进行,要取消请走争议流程");
            case CONFIRMED, PENDING ->{
                  order.setStatus(OrderStatus.CANCELLED);
                  order.setCancelledAt(LocalDateTime.now());
                  order.setCancelReason("管理员强制取消:" + request.getReason());
                  weddingOrderRepository.save(order);
                  writeLog(order.getId(), from, OrderStatus.CANCELLED, adminId, "ADMIN",
                           "管理员强制取消订单:" + request.getReason());
                  releaseSchedule(order.getServiceId(), order.getWeddingDate());
                    return order;
            }
            default ->
                throw new BusinessException("状态不合法");


        }
    }

    // 释放档期:单子取消后调用 —— 该服务该日已无任何未了结的单时,把档期状态置回 1(可预约)。
    // 幂等:本来就是 1 再置一次没有副作用,所以不判断当前值。
    // 先判"还有没有别的有效单"再置位,是因为同一天可能先被商家拒、又被另一单重新占上。
    private void releaseSchedule(Long serviceId, LocalDate date) {
        if (serviceId == null || date == null) {
            return;
        }
        if (weddingOrderRepository.existsByServiceIdAndWeddingDateAndStatusIn(serviceId, date, OCCUPYING_STATUS)) {
            return;
        }
        serviceScheduleRepository.findByServiceIdAndScheduleDate(serviceId, date)
                .filter(s -> s.getStatus() == null || s.getStatus() == 0)
                .ifPresent(s -> {
                    s.setStatus(1);
                    serviceScheduleRepository.save(s);
                });
    }

    // 写一条状态流转记录(私有小工具,三处共用,避免重复)
    private void writeLog(Long orderId, OrderStatus from, OrderStatus to, Long operatorId, String role, String remark) {
        OrderStatusLog log = new OrderStatusLog();
        log.setOrderId(orderId);
        log.setFromStatus(from);
        log.setToStatus(to);
        log.setOperatorId(operatorId);
        log.setOperatorRole(role);
        log.setRemark(remark);
        orderStatusLogRepository.save(log);
    }
}
