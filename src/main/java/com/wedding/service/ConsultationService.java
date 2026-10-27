package com.wedding.service;

import com.wedding.dto.SendMessageRequest;
import com.wedding.dto.StartConsultationRequest;
import com.wedding.entity.ConsultMessage;
import com.wedding.entity.Consultation;

import java.util.List;

// 厨师:在线咨询这道菜(需求 FR-U-04-1 / FR-M-04)。
//
// 会话按 (用户, 商家) 一对一个:同一对只存在一个会话(uk_user_merchant),重复发起复用。
public interface ConsultationService {

    // 用户发起咨询:按 (用户, 商家) 取已有会话,没有就新建(get-or-create)
    Consultation startConsultation(Long userId, StartConsultationRequest request);

    // 用户:我的会话列表,按最近消息时间倒序
    List<Consultation> listUserConsultations(Long userId);

    // 用户:读某会话的消息(把商家发来的标已读、清我的未读)
    List<ConsultMessage> listUserMessages(Long userId, Long consultationId);

    // 用户:发消息(只能是会话里的用户)
    ConsultMessage sendUserMessage(Long userId, Long consultationId, SendMessageRequest request);

    // 商家:我的会话列表,按最近消息时间倒序
    List<Consultation> listMerchantConsultations(Long userId);

    // 商家:读某会话的消息(把用户发来的标已读、清我的未读)
    List<ConsultMessage> listMerchantMessages(Long userId, Long consultationId);

    // 商家:回复消息(只能回自己店铺的会话)
    ConsultMessage sendMerchantMessage(Long userId, Long consultationId, SendMessageRequest request);
}
