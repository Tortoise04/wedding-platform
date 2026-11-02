package com.wedding.service.impl;

import com.wedding.dto.SendMessageRequest;
import com.wedding.dto.StartConsultationRequest;
import com.wedding.entity.ConsultMessage;
import com.wedding.entity.Consultation;
import com.wedding.entity.Merchant;
import com.wedding.entity.User;
import com.wedding.entity.WeddingOrder;
import com.wedding.entity.WeddingService;
import com.wedding.exception.BusinessException;
import com.wedding.repository.ConsultMessageRepository;
import com.wedding.repository.ConsultationRepository;
import com.wedding.repository.MerchantRepository;
import com.wedding.repository.UserRepository;
import com.wedding.repository.WeddingOrderRepository;
import com.wedding.repository.WeddingServiceRepository;
import com.wedding.service.ChatPushService;
import com.wedding.service.ConsultationService;
import com.wedding.service.MerchantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// 厨师:在线咨询(用户与商家共用的会话 / 消息)。
//
// 会话是"创建",消息是"追加";每次发消息都要顺带更新会话的最后一条摘要 / 时间 / 对方未读数,
// 所以"追加消息 + 更新会话"同处一个 @Transactional,避免"消息进了、列表却看不到"的半截状态。
@Service
@RequiredArgsConstructor
public class ConsultationServiceImpl implements ConsultationService {

    private final ConsultationRepository consultationRepository;
    private final ConsultMessageRepository consultMessageRepository;
    private final MerchantService merchantService;
    private final UserRepository userRepository;
    private final MerchantRepository merchantRepository;
    private final WeddingServiceRepository serviceRepository;
    private final WeddingOrderRepository orderRepository;
    private final ChatPushService chatPushService;

    // 用户发起咨询:先确认商家可咨询(已过审且启用),再按 (用户, 商家) get-or-create,
    // 并把这回来的来意(订单 / 服务)盖成「最近一次来源」—— 会话头上那张卡片显示的就是它。
    @Transactional
    @Override
    public Consultation startConsultation(Long userId, StartConsultationRequest request) {
        if (request.getMerchantId() == null) {
            throw new BusinessException("被咨询商家不能为空");
        }
        merchantService.getMerchantById(request.getMerchantId());   // 不存在 / 未过审 / 已停用都会 400

        // 请求带订单时先把订单取出来验一遍:来源卡片要显示这单的单号 / 金额 / 状态,
        // 而且必须确认「这单确实是这个人在这家店下的」——少了这道校验,
        // 随便谁把别人的订单 id 塞进来,自己的会话头上就会挂着别人的单号与金额。
        WeddingOrder sourceOrder = null;
        if (request.getOrderId() != null) {
            sourceOrder = orderRepository.findById(request.getOrderId())
                    .orElseThrow(() -> new BusinessException("订单不存在: " + request.getOrderId()));
            if (!userId.equals(sourceOrder.getUserId())
                    || !request.getMerchantId().equals(sourceOrder.getMerchantId())) {
                throw new BusinessException("该订单不属于你与该商家，不能作为咨询来源");
            }
        }

        // t_consultation 上有 uk_user_merchant:这一对人多点几次「咨询」也只能有一张会话,
        // 所以按 (用户, 商家) 二元组找,找到就复用、找不到才新建。
        // 复用而不是新建,是为了让「最近一次来源」有地方可盖 —— 新开一张会直接撞唯一键(500)。
        Consultation c = consultationRepository.findByUserIdAndMerchantId(userId, request.getMerchantId())
                .orElseGet(() -> {
                    Consultation fresh = new Consultation();
                    fresh.setUserId(userId);
                    fresh.setMerchantId(request.getMerchantId());
                    fresh.setUserUnread(0);
                    fresh.setMerchantUnread(0);
                    return consultationRepository.save(fresh);
                });

        // 来源覆盖(只记最近一次):带订单就以订单为准,只带服务就换服务,
        // 两者都不带(从会话列表直接点「发消息」)就原样不动 —— 上回的来意还挂着。
        if (sourceOrder != null) {
            c.setOrderId(sourceOrder.getId());
            c.setServiceId(sourceOrder.getServiceId());   // 订单本就绑着一项服务,顺带把服务来源补齐
            consultationRepository.save(c);
        } else if (request.getServiceId() != null) {
            c.setServiceId(request.getServiceId());       // 只换服务:之前记的订单不动
            consultationRepository.save(c);
        }

        // 刚发起就要把卡片字段填好:前端拿到返回值直接渲染会话头,不该再为一次跳转多打一个查询
        enrichSource(List.of(c));
        return c;
    }

    // 用户:我的会话列表。
    @Override
    public List<Consultation> listUserConsultations(Long userId) {
        List<Consultation> list = consultationRepository.findByUserIdOrderByLastTimeDesc(userId);
        enrichSource(list);
        return list;
    }

    // 用户:读消息 —— 校验会话归属,把商家发来的标已读并清我的未读。
    @Transactional
    @Override
    public List<ConsultMessage> listUserMessages(Long userId, Long consultationId) {
        Consultation c = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new BusinessException("会话不存在: " + consultationId));
        if (!c.getUserId().equals(userId)) {
            throw new BusinessException("无权查看该会话");
        }
        return readMessages(c, userId);
    }

    // 用户:发消息 —— 校验归属,追加消息并刷新会话摘要 / 商家未读。
    @Transactional
    @Override
    public ConsultMessage sendUserMessage(Long userId, Long consultationId, SendMessageRequest request) {
        Consultation c = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new BusinessException("会话不存在: " + consultationId));
        if (!c.getUserId().equals(userId)) {
            throw new BusinessException("无权在该会话发言");
        }
        return appendMessage(c, userId, "USER", request);
    }

    // 商家:我的会话列表(按店铺解析身份)。
    @Override
    public List<Consultation> listMerchantConsultations(Long userId) {
        Merchant shop = merchantService.getMyShop(userId);
        List<Consultation> list = consultationRepository.findByMerchantIdOrderByLastTimeDesc(shop.getId());
        // 补上咨询方用户名:列表与会话头要根据它显示"和谁在聊",只回 userId 商家认不出人
        for (Consultation c : list) {
            userRepository.findById(c.getUserId()).ifPresent(u -> c.setUserName(displayName(u)));
        }
        enrichSource(list);
        return list;
    }

    // 展示名:优先真名,没填真名退回用户名(演示账号 real_name 为 null,显示的就是 username)
    private String displayName(User u) {
        if (u == null) {
            return null;
        }
        String real = u.getRealName();
        return (real != null && !real.isBlank()) ? real : u.getUsername();
    }

    // 商家:读消息 —— 校验店铺归属,把用户发来的标已读并清我的未读。
    @Transactional
    @Override
    public List<ConsultMessage> listMerchantMessages(Long userId, Long consultationId) {
        Merchant shop = merchantService.getMyShop(userId);
        Consultation c = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new BusinessException("会话不存在: " + consultationId));
        if (!c.getMerchantId().equals(shop.getId())) {
            throw new BusinessException("无权查看该会话");
        }
        return readMessages(c, userId);
    }

    // 商家:回复消息 —— 校验店铺归属,追加消息并刷新会话摘要 / 用户未读。
    @Transactional
    @Override
    public ConsultMessage sendMerchantMessage(Long userId, Long consultationId, SendMessageRequest request) {
        Merchant shop = merchantService.getMyShop(userId);
        Consultation c = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new BusinessException("会话不存在: " + consultationId));
        if (!c.getMerchantId().equals(shop.getId())) {
            throw new BusinessException("无权在该会话回复");
        }
        return appendMessage(c, userId, "MERCHANT", request);
    }

    // 私有工具:读消息并把"对方发的"标已读,同时清掉我这一侧的未读数。
    private List<ConsultMessage> readMessages(Consultation c, Long myUserId) {
        List<ConsultMessage> messages = consultMessageRepository.findByConsultationIdOrderByCreatedAtAsc(c.getId());
        for (ConsultMessage m : messages) {
            if (!m.getSenderId().equals(myUserId) && (m.getIsRead() == null || m.getIsRead() != 1)) {
                m.setIsRead(1);
            }
        }
        consultMessageRepository.saveAll(messages);
        // 我这一侧未读清零(用户清 userUnread,商家清 merchantUnread)
        if (c.getUserId().equals(myUserId)) {
            c.setUserUnread(0);
        } else {
            c.setMerchantUnread(0);
        }
        consultationRepository.save(c);
        return messages;
    }

    // 私有工具:追加一条消息,并刷新会话的最后摘要 / 时间 / 对方未读数,最后把新消息推给双方。
    private ConsultMessage appendMessage(Consultation c, Long senderId, String role, SendMessageRequest request) {
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new BusinessException("消息内容不能为空");
        }
        if (request.getContent().length() > 1000) {
            throw new BusinessException("消息内容不能超过 1000 字");   // 列宽 varchar(1000),超了会 500
        }
        ConsultMessage m = new ConsultMessage();
        m.setConsultationId(c.getId());
        m.setSenderId(senderId);
        m.setSenderRole(role);
        m.setContent(request.getContent());
        m.setIsRead(0);
        consultMessageRepository.save(m);

        c.setLastMessage(request.getContent());
        c.setLastTime(LocalDateTime.now());
        if ("USER".equals(role)) {
            c.setMerchantUnread(c.getMerchantUnread() + 1);   // 用户发 → 商家未读 +1
        } else {
            c.setUserUnread(c.getUserUnread() + 1);           // 商家发 → 用户未读 +1
        }
        consultationRepository.save(c);

        // 推送目标只有 userId,会话里存的却是 t_merchant.id —— 商家那一侧的 userId 得从店铺上找出来。
        // 店铺理论上一定在(会话就是照它建出来的),真取不到就只推用户侧,不为一声通知把发消息搞挂。
        Long merchantUserId = merchantRepository.findById(c.getMerchantId())
                .map(Merchant::getUserId)
                .orElse(null);
        // 双方都推:对方要看到新消息,发送方自己的其它标签页也要跟着上屏(前端按 message.id 去重)。
        // 交给 pushMessageAfterCommit 而不是直接推:此刻还坐在 @Transactional 里,
        // 真现在推出去,对方一回拉读到的是提交前的旧未读数,消息与列表会对不上。
        chatPushService.pushMessageAfterCommit(c.getId(), m, c.getUserId(), merchantUserId);
        return m;
    }

    // 私有工具:给会话补上「来源卡片」的 6 个展示字段(服务名 / 封面 / 价格、订单号 / 金额 / 状态)。
    //
    // 一律按 id 批量取(两个 findAllById),不在循环里查库 —— 列表一页二十条会话,
    // 逐条回查就是四十趟往返,冷库的门一会儿开一会儿关,页面转圈转得让人以为断网了。
    private void enrichSource(List<Consultation> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Set<Long> orderIds = new HashSet<>();
        for (Consultation c : list) {
            if (c.getOrderId() != null) {
                orderIds.add(c.getOrderId());
            }
        }
        Map<Long, WeddingOrder> orders = new HashMap<>();
        for (WeddingOrder o : orderRepository.findAllById(orderIds)) {
            orders.put(o.getId(), o);
        }

        // 服务按「订单优先」取:会话记了订单时,卡片上的服务就该是订单上那项服务,
        // 否则订单绑的服务与服务详情点的服务不一致时,同一张卡片会自相矛盾。
        Set<Long> serviceIds = new HashSet<>();
        for (Consultation c : list) {
            Long sid = serviceIdOf(c, orders);
            if (sid != null) {
                serviceIds.add(sid);
            }
        }
        Map<Long, WeddingService> services = new HashMap<>();
        for (WeddingService s : serviceRepository.findAllById(serviceIds)) {
            services.put(s.getId(), s);
        }

        for (Consultation c : list) {
            WeddingOrder o = c.getOrderId() == null ? null : orders.get(c.getOrderId());
            Long sid = serviceIdOf(c, orders);
            WeddingService s = sid == null ? null : services.get(sid);
            // 服务被删 / 订单被清理就留 null,前端按占位显示 —— 一条脏数据不该把整个列表拉成 500
            if (s != null) {
                c.setSourceServiceName(s.getName());
                c.setSourceServiceCover(s.getCoverUrl());
                c.setSourceServicePrice(s.getPrice());
            }
            if (o != null) {
                c.setSourceOrderNo(o.getOrderNo());
                c.setSourceOrderAmount(o.getAmount());
                c.setSourceOrderStatus(o.getStatus() == null ? null : o.getStatus().getDesc());
            }
        }
    }

    // 私有工具:这条会话该展示哪项服务 —— 有订单就看订单绑的服务,没有才退回会话自己的 service_id。
    private Long serviceIdOf(Consultation c, Map<Long, WeddingOrder> orders) {
        WeddingOrder o = c.getOrderId() == null ? null : orders.get(c.getOrderId());
        return o != null ? o.getServiceId() : c.getServiceId();
    }
}
