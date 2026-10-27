package com.wedding.controller;

import com.wedding.annotation.RequireMerchant;
import com.wedding.common.Result;
import com.wedding.dto.SendMessageRequest;
import com.wedding.entity.ConsultMessage;
import com.wedding.entity.Consultation;
import com.wedding.service.ConsultationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 服务员:商家端 · 在线咨询(路径 /api/merchant/consultations,均 @RequireMerchant)。
@RestController
@RequestMapping("/api/merchant/consultations")
@RequiredArgsConstructor
public class MerchantConsultationController {

    private final ConsultationService consultationService;

    // 本店铺的咨询会话列表
    @RequireMerchant
    @GetMapping
    public Result<List<Consultation>> list(@RequestAttribute("userId") Long userId) {
        return Result.success(consultationService.listMerchantConsultations(userId));
    }

    // 读某会话的消息(同时把用户发来的标已读)
    @RequireMerchant
    @GetMapping("/{id}/messages")
    public Result<List<ConsultMessage>> messages(@PathVariable Long id,
                                                 @RequestAttribute("userId") Long userId) {
        return Result.success(consultationService.listMerchantMessages(userId, id));
    }

    // 回复消息
    @RequireMerchant
    @PostMapping("/{id}/messages")
    public Result<ConsultMessage> reply(@PathVariable Long id,
                                        @RequestBody SendMessageRequest request,
                                        @RequestAttribute("userId") Long userId) {
        return Result.success(consultationService.sendMerchantMessage(userId, id, request));
    }
}
