package com.wedding.service.impl;

import com.wedding.dto.SendMessageRequest;
import com.wedding.dto.StartConsultationRequest;
import com.wedding.entity.ConsultMessage;
import com.wedding.entity.Consultation;
import com.wedding.entity.Merchant;
import com.wedding.entity.User;
import com.wedding.exception.BusinessException;
import com.wedding.repository.ConsultMessageRepository;
import com.wedding.repository.ConsultationRepository;
import com.wedding.repository.UserRepository;
import com.wedding.service.ConsultationService;
import com.wedding.service.MerchantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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

    // 用户发起咨询:先确认商家可咨询(已过审且启用),再按 (用户, 商家) get-or-create。
    @Transactional
    @Override
    public Consultation startConsultation(Long userId, StartConsultationRequest request) {
        if (request.getMerchantId() == null) {
            throw new BusinessException("被咨询商家不能为空");
        }
        merchantService.getMerchantById(request.getMerchantId());   // 不存在 / 未过审 / 已停用都会 400

        // 门户重复点「咨询商家」不该裂出多个会话:先按 (用户, 商家, 服务) 三元组找,
        // 找不到再退回 (用户, 商家) —— t_consultation 上有 uk_user_merchant,
        // 这一对人有会话时再插一条会直接撞唯一键(500),所以必须复用已有的那条。
        Long serviceId = request.getServiceId();
        return (serviceId == null
                ? consultationRepository.findByUserIdAndMerchantId(userId, request.getMerchantId())
                : consultationRepository
                        .findByUserIdAndMerchantIdAndServiceId(userId, request.getMerchantId(), serviceId)
                        .or(() -> consultationRepository.findByUserIdAndMerchantId(userId, request.getMerchantId())))
                .orElseGet(() -> {
                    Consultation c = new Consultation();
                    c.setUserId(userId);
                    c.setMerchantId(request.getMerchantId());
                    c.setServiceId(request.getServiceId());
                    c.setUserUnread(0);
                    c.setMerchantUnread(0);
                    return consultationRepository.save(c);
                });
    }

    // 用户:我的会话列表。
    @Override
    public List<Consultation> listUserConsultations(Long userId) {
        return consultationRepository.findByUserIdOrderByLastTimeDesc(userId);
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

    // 私有工具:追加一条消息,并刷新会话的最后摘要 / 时间 / 对方未读数。
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
        return m;
    }
}
