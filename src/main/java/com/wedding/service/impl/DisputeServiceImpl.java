package com.wedding.service.impl;


import com.wedding.dto.AcceptDisputeRequest;
import com.wedding.dto.ArbitrateDisputeRequest;
import com.wedding.dto.CreateDisputeRequest;
import com.wedding.dto.SubmitEvidenceRequest;
import com.wedding.entity.Dispute;
import com.wedding.entity.DisputeEvidence;
import com.wedding.entity.Merchant;
import com.wedding.entity.OrderStatusLog;
import com.wedding.entity.Penalty;
import com.wedding.entity.WeddingOrder;
import com.wedding.enums.DisputeStatus;
import com.wedding.enums.EvidenceType;
import com.wedding.enums.OrderStatus;
import com.wedding.enums.PenaltyType;
import com.wedding.exception.BusinessException;
import com.wedding.repository.DisputeEvidenceRepository;
import com.wedding.repository.DisputeRepository;
import com.wedding.repository.MerchantRepository;
import com.wedding.repository.OrderStatusLogRepository;
import com.wedding.repository.PenaltyRepository;
import com.wedding.repository.WeddingOrderRepository;
import com.wedding.service.DisputeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

// 厨师:平台争议 / 仲裁这道菜(FR-A-03)。
//
// 争议状态机:PENDING → PROCESSING → ARBITRATING → FINISHED,受理驳回则 PENDING → CLOSED。
// 最重的 arbitrate 要同时改 t_dispute / t_order / t_order_status_log,必须同一事务。
@Service
@RequiredArgsConstructor
public class DisputeServiceImpl implements DisputeService {
    private final WeddingOrderRepository weddingOrderRepository;
    private final DisputeRepository disputeRepository;
    // 举证身份判定用:从当前登录人反查店铺,看是不是被投诉商家
    private final MerchantRepository merchantRepository;
    // 举证材料表存取
    private final DisputeEvidenceRepository disputeEvidenceRepository;
    // 裁决联动用:订单状态流转留痕,写进 t_order_status_log
    private final OrderStatusLogRepository orderStatusLogRepository;
    // 裁决联动用:结论为 PENALTY 时往 t_penalty 插一条处罚记录
    private final PenaltyRepository penaltyRepository;



    // 用户发起平台介入:建一条待受理(PENDING)争议单,★ 订单状态保持不变。
    //
    // 三道闸门:只能对自己的订单发起(归属校验);订单须已接单
    // (CONFIRMED / IN_PROGRESS / COMPLETED);一单一争议(已发起过就拦)。
    // 同一订单的争议发起串行化:订单行锁负责跨事务,进程内锁只兜同实例连点
    // (t_dispute.order_id 只有普通索引,没有唯一索引兜底,所以这里必须靠行锁)。
    private static final java.util.concurrent.ConcurrentHashMap<String, Object> DISPUTE_LOCKS = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    @Transactional
    public Dispute createDispute(Long userId, CreateDisputeRequest request) {
        if (request.getOrderId() == null) {
            throw new BusinessException("请选择要发起介入的订单");
        }
        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new BusinessException("请填写介入诉求");
        }
        if (request.getReason().length() > 500) {
            throw new BusinessException("诉求内容不能超过 500 字");
        }
        // 1. 查订单
        WeddingOrder order = weddingOrderRepository.findByIdForUpdate(request.getOrderId())
                .orElseThrow(() -> new BusinessException("订单不存在: " + request.getOrderId()));

        // 2. 归属校验:只能对自己的订单发起
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该订单");
        }

        // 3. 前置状态校验:商家已接单之后(已确认 / 履约中 / 已完成)才能发起争议
        OrderStatus st = order.getStatus();
        if (st != OrderStatus.CONFIRMED && st != OrderStatus.IN_PROGRESS && st != OrderStatus.COMPLETED) {
            throw new BusinessException("该订单当前状态不满足发起争议的条件");
        }

        synchronized (DISPUTE_LOCKS.computeIfAbsent("dispute:" + order.getId(), k -> new Object())) {
            // 4. 幂等:一个订单只能发起一次
            if (disputeRepository.existsByOrderId(order.getId())) {
                throw new BusinessException("该订单已发起过平台介入");
            }

            // 5. 建争议单(★ 注意:订单状态【不变】)
            Dispute d = new Dispute();
            d.setDisputeNo("D" + System.currentTimeMillis() + (int)(Math.random() * 10000));
            d.setOrderId(order.getId());
            d.setUserId(userId);
            d.setMerchantId(order.getMerchantId());      // 被投诉商家来自订单
            d.setReason(request.getReason());
            d.setStatus(DisputeStatus.PENDING);          // 待受理
            Dispute saved = disputeRepository.save(d);

            // 首发就能带图:同一事务里补一条 IMAGE 举证。
            // 不复用 submitEvidence —— 它只收 PROCESSING 状态的争议,而此刻争议刚落库、还是 PENDING,
            // 走那条路必然被"当前不在举证环节"拦掉,用户上传的图就白丢了。
            // t_dispute 不加列(后端按需从举证表取),接口返回体仍是 Dispute。
            if (!blank(request.getFileUrl())) {
                DisputeEvidence evidence = new DisputeEvidence();
                evidence.setDisputeId(saved.getId());
                evidence.setSubmitterId(userId);          // 发起争议的就是举证的用户
                evidence.setSubmitterRole("USER");
                evidence.setEvidenceType(EvidenceType.IMAGE);
                evidence.setFileUrl(request.getFileUrl().trim());
                disputeEvidenceRepository.save(evidence);
            }
            return saved;
        }
    }

    // 争议单只存 order_id,前台要显示人能认的订单号(O1792...),列表/详情统一在这里回填。
    // @Transient 字段,不回写数据库;订单查不到(脏数据)时留空,前端退回展示 #id。
    private void fillOrderNo(List<Dispute> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Set<Long> orderIds = list.stream().map(Dispute::getOrderId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (orderIds.isEmpty()) {
            return;
        }
        Map<Long, String> noMap = weddingOrderRepository.findAllById(orderIds).stream()
                .collect(Collectors.toMap(WeddingOrder::getId, WeddingOrder::getOrderNo, (a, b) -> a));
        list.forEach(d -> d.setOrderNo(noMap.get(d.getOrderId())));
    }

    // 用户端:我的争议列表(按发起时间倒序)
    @Override
    public List<Dispute> listMyDisputes(Long userId) {
        List<Dispute> list = disputeRepository.findByUserIdOrderByCreatedAtDesc(userId);
        fillOrderNo(list);
        return list;
    }

    // 商家端:针对本店的争议列表(先按登录人解析店铺,再按店铺倒序取争议)
    @Override
    public List<Dispute> listShopDisputes(Long userId) {
        Merchant shop = merchantRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("请先完成商家入驻"));
        List<Dispute> list = disputeRepository.findByMerchantIdOrderByCreatedAtDesc(shop.getId());
        fillOrderNo(list);
        return list;
    }

    // 用户端:当事人查看争议详情(纯查询,不加状态约束)
    @Override
    public Dispute getMyDispute(Long disputeId, Long userId) {
        Dispute dispute = requireParty(disputeId, userId);
        fillOrderNo(List.of(dispute));
        return dispute;
    }

    // 用户端:当事人查看举证材料,按创建时间正序(聊天流从早到晚)
    @Override
    public List<DisputeEvidence> listMyEvidences(Long disputeId, Long userId) {
        Dispute dispute = requireParty(disputeId, userId);
        return disputeEvidenceRepository.findByDisputeIdOrderByCreatedAtAsc(dispute.getId());
    }

    // 当事人校验,两个只读接口共用:先查争议单,再验当前登录人是不是发起人或被投诉店铺店主
    private Dispute requireParty(Long disputeId, Long userId) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new BusinessException("争议单不存在: " + disputeId));
        // 情形一:当前登录人就是争议发起人
        if (userId.equals(dispute.getUserId())) {
            return dispute;
        }
        // 情形二:当前登录人名下有店铺,且该店铺正是被投诉商家
        Merchant shop = merchantRepository.findByUserId(userId).orElse(null);
        if (shop != null && shop.getId().equals(dispute.getMerchantId())) {
            return dispute;
        }
        throw new BusinessException("无权查看该争议");
    }


    // 受理判断(FR-A-03-1):争议状态机第一次流转,带一条岔路。
    //
    // approved = true  → PENDING 改成 PROCESSING(举证中),记 accepted_at = 现在;
    // approved = false → PENDING 改成 CLOSED(已关闭),原因写进 result。
    // 只有"待受理"(PENDING)能受理,其余状态一律拦下。
    @Transactional
    @Override
    public Dispute acceptDispute(Long disputeId, Long adminId, AcceptDisputeRequest request) {

        // 1. 查争议单,查不到即拦
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new BusinessException("争议订单不存在: " + disputeId));
        // 2. 前置状态校验:只有"待受理"(PENDING)能受理,其余一律拦下
        DisputeStatus st = dispute.getStatus();
        if(st != DisputeStatus.PENDING){
            throw new BusinessException("该争议单当前状态不允许受理");
        }
        // 3. approved 为 null 当非法参数拦下;并记 handler_id = adminId
        if (request.getApproved()==null){
            throw new BusinessException("参数不合法");
        }
        dispute.setHandlerId(adminId);

        if (request.getApproved()) {
            dispute.setStatus(DisputeStatus.PROCESSING);
            dispute.setAcceptedAt(LocalDateTime.now());
        }else{
            String reason = request.getRejectReason();
            if (reason == null || reason.isBlank()) {           // 空串和 null 一起拦
                throw new BusinessException("不予受理必须说明原因");
            }
            dispute.setStatus(DisputeStatus.CLOSED);
            dispute.setResult(request.getRejectReason());


        }

        // 4. 保存
        return disputeRepository.save(dispute);


    }


    // 管理端 · 争议列表:可选按状态筛选,按创建时间倒序(纯查询)
    @Override
    public List<Dispute> listDisputes(DisputeStatus status) {
        List<Dispute> list = status == null
                ? disputeRepository.findAllByOrderByCreatedAtDesc()
                : disputeRepository.findByStatusOrderByCreatedAtDesc(status);
        fillOrderNo(list);
        return list;
    }


    // 双方举证(FR-A-03-2):往 t_dispute_evidence 新增一条材料。
    //
    // ★ 核心是身份判定:用户与商家共用本接口,服务端要自己认出发起人是哪一方,
    // 写进 submitter_role(USER / MERCHANT),前端传的不可信。
    // ⚠️ 举证只补材料、不推进状态,何时转"仲裁中"由 finishEvidence 决定。
    @Transactional
    @Override
    public DisputeEvidence submitEvidence(Long disputeId, Long submitterId, SubmitEvidenceRequest request) {

        // 1. 查争议单,查不到即拦
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new BusinessException("争议单不存在: " + disputeId));
        // 2. 前置状态校验:只有 PROCESSING(举证中)能提交材料
        if (dispute.getStatus()!=DisputeStatus.PROCESSING){
            throw new BusinessException("该争议单当前不在举证环节");
        }
        // 3. ★ 身份判定:是发起用户 → USER;是被投诉店铺店主 → MERCHANT;都不是 → 无权举证
        String role;
        if (submitterId.equals(dispute.getUserId())) {
            role = "USER";
        } else {
            Merchant shop = merchantRepository.findByUserId(submitterId).orElse(null);
            if (shop != null && shop.getId().equals(dispute.getMerchantId())) {
                role = "MERCHANT";
            } else {
                throw new BusinessException("你不是该争议的当事人，无权举证");
            }
        }

        // 4. ★ 材料类型服务端推导:文字与图片都能填,纯空白按没填算,类型绝不从请求体取
        boolean hasText = !blank(request.getContent());
        boolean hasFile = !blank(request.getFileUrl());
        if (!hasText && !hasFile) {
            throw new BusinessException("请至少填写文字说明或上传图片");
        }
        // 两者都有是 MIXED,只有一个是 TEXT / IMAGE
        EvidenceType type = hasText ? (hasFile ? EvidenceType.MIXED : EvidenceType.TEXT) : EvidenceType.IMAGE;

        // 5. 建材料(创建类操作:new + set + save;created_at 由 @CreationTimestamp 自动填)
        DisputeEvidence evidence = new DisputeEvidence();
        evidence.setDisputeId(disputeId);
        evidence.setSubmitterId(submitterId);
        evidence.setSubmitterRole(role);
        evidence.setEvidenceType(type);
        // 空白项存 null,避免库里出现空串和类型对不上的脏数据
        evidence.setContent(hasText ? request.getContent().trim() : null);
        evidence.setFileUrl(hasFile ? request.getFileUrl().trim() : null);

        return disputeEvidenceRepository.save(evidence);


    }

    // 判空口径:null、空串、纯空白都算没填
    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    // 查看某争议的全部举证材料(纯查询,按创建时间正序)
    @Override
    public List<DisputeEvidence> listEvidences(Long disputeId) {
        return disputeEvidenceRepository.findByDisputeIdOrderByCreatedAtAsc(disputeId);
    }

    // 举证完毕 → 转"仲裁中"(PROCESSING → ARBITRATING),仅推进状态
    @Transactional
    @Override
    public Dispute finishEvidence(Long disputeId) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new BusinessException("争议单不存在: " + disputeId));

        if (dispute.getStatus() != DisputeStatus.PROCESSING) {
            throw new BusinessException("该争议单当前不处于举证阶段");
        }

        dispute.setStatus(DisputeStatus.ARBITRATING);   // 举证中 → 仲裁中
        // ⚠️ handler_id 不在这里改:它的语义是"受理的管理员"(受理时已赋值),
        //    这里只推进状态,不动记名。
        return disputeRepository.save(dispute);
    }


    // 作出裁决(FR-A-03-3):争议链里最重的一步,第一次同时改多张表。
    //
    // t_dispute(结论)+ t_order(退款 / 关单要取消订单)+ t_order_status_log(留痕)必须同生共死,
    // 否则会出现"钱退了、订单还挂着"的脏数据 —— 这正是本方法 @Transactional 的意义。
    // 只有"仲裁中"(ARBITRATING)能裁决;结论分 REFUND / CLOSE_ORDER / REJECT / PENALTY 四路。
    @Transactional
    @Override
    public Dispute arbitrate(Long disputeId, Long adminId, ArbitrateDisputeRequest request) {

        // 1. 查争议单,查不到即拦
        Dispute dispute = disputeRepository.findById(disputeId)
                    .orElseThrow(() -> new BusinessException("争议单不存在: " + disputeId));

        // 2. 前置状态校验:只有 ARBITRATING 能裁决
        // ⚠️ 用争议自己的枚举 DisputeStatus,别手滑写成 OrderStatus
        if (dispute.getStatus() != DisputeStatus.ARBITRATING) {
            throw new BusinessException("该争议单当前不处于仲裁阶段");
        }

        // 3. 结论不能为空
        if (request.getResultType()==null){
            throw new BusinessException("裁决结论不能为空");
        }
        // 4. 先落争议自身的公共字段(各路都要写)
            dispute.setResultType(request.getResultType());
            dispute.setResult(request.getResult());            // 裁决说明,给双方看
            dispute.setFinishedAt(LocalDateTime.now());
            dispute.setStatus(DisputeStatus.FINISHED);         // 已裁决

        // 5. ★ 按结论执行订单联动(本方法核心,多表写入就在这一段)
            switch (request.getResultType()) {
                case REFUND:
                    // 5.1 退款金额必须 > 0
                    if (request.getRefundAmount() == null || request.getRefundAmount().compareTo(BigDecimal.ZERO) <= 0) {
                        throw new BusinessException("退款金额异常");
                    }
                    // 5.2 记退款金额
                    dispute.setRefundAmount(request.getRefundAmount());
                    // 5.3 联动取消订单(私有工具:改单 + 写流水)
                    cancelOrderByArbitration(dispute.getOrderId(), adminId, "平台裁决：退款，订单取消");
                    break;
                case CLOSE_ORDER:
                    // 联动取消订单
                    cancelOrderByArbitration(dispute.getOrderId(), adminId, "平台裁决：关闭订单，订单取消");
                    break;
                case REJECT:
                    // REJECT:订单维持原状,什么都不做
                    break;
                case PENALTY:
                    // PENALTY:不动订单,改为新增一条处罚记录(new + save)
                    // 1. 处罚类型不能为空
                    if (request.getPenaltyType()==null){
                        throw new BusinessException("处罚类型不能为空");
                    }

                    // 2. 只有 SERVICE_OFF 必须指定 targetId(下架哪个服务);
                    //    警告 / 封禁针对整个店铺,targetId 允许为空
                    if (request.getPenaltyType()==PenaltyType.SERVICE_OFF && request.getTargetId()==null){
                        throw new BusinessException("服务下架必须指定要下架的服务");
                    }
                    // 3. new + set + save(★ 被罚对象是商家:merchantId = dispute.getMerchantId())。
                    //    千万别写成 dispute.getUserId(),那是发起争议的用户;
                    //    罚错对象是这一步最严重的错误(和举证里混淆身份同一类坑)。
                    Penalty penalty =new Penalty();
                    penalty.setMerchantId(dispute.getMerchantId());
                    penalty.setOperatorId(adminId);
                    penalty.setPenaltyType(request.getPenaltyType());
                    if (request.getPenaltyType()==PenaltyType.SERVICE_OFF){
                        penalty.setTargetId(request.getTargetId());
                    }
                    penalty.setReason(request.getResult());
                    penalty.setStartTime(LocalDateTime.now());
                    penalty.setEndTime(request.getPenaltyEndTime());
                    penalty.setStatus(1);

                    penaltyRepository.save(penalty);

                    // ⚠️ 处罚不碰订单:订单归 REFUND / CLOSE_ORDER 管,
                    //    罚商家和取消订单是两件事,别顺手把订单也取消了
                    break;
                default:
                    // 枚举已被穷举,正常到不了这里;兜底防将来新增结论忘了处理
                    throw new BusinessException("暂不支持的裁决结论");
            }

            return disputeRepository.save(dispute);


    }

    // 平台裁决导致订单取消时的私有工具:把订单改成"已取消"并写一条流转流水。
    //
    // 退款 / 关单两路共用;它内部的 save 与调用方的争议 save 同处一个事务,
    // 所以"争议已裁决、订单没取消"这种半截状态不会发生。
    private void cancelOrderByArbitration(Long orderId, Long adminId, String remark) {
        WeddingOrder order = weddingOrderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("订单不存在: " + orderId));

        OrderStatus from = order.getStatus();                 // 先记住"从哪个状态来的"
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancelReason(remark);
        weddingOrderRepository.save(order);

        OrderStatusLog log = new OrderStatusLog();
        log.setOrderId(order.getId());
        log.setFromStatus(from);
        log.setToStatus(OrderStatus.CANCELLED);
        log.setOperatorId(adminId);
        // 记名:是管理员裁决造成的取消
        log.setOperatorRole("ADMIN");
        log.setRemark(remark);
        orderStatusLogRepository.save(log);
    }
}
