package com.wedding.controller;

import com.wedding.annotation.RequireLogin;
import com.wedding.common.Result;
import com.wedding.dto.SendMessageRequest;
import com.wedding.dto.StartConsultationRequest;
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

// 服务员:用户端 · 在线咨询(路径 /api/consultations,均 @RequireLogin)。
@RestController
@RequestMapping("/api/consultations")
@RequiredArgsConstructor
public class ConsultationController {

    private final ConsultationService consultationService;

    // 发起咨询:按 (我, 商家) 取已有会话,没有就新建
    @RequireLogin
    @PostMapping
    public Result<Consultation> start(@RequestBody StartConsultationRequest request,
                                      @RequestAttribute("userId") Long userId) {
        return Result.success(consultationService.startConsultation(userId, request));
    }

    // 我的会话列表
    @RequireLogin
    @GetMapping
    public Result<List<Consultation>> list(@RequestAttribute("userId") Long userId) {
        return Result.success(consultationService.listUserConsultations(userId));
    }

    // 读某会话的消息(同时把商家发来的标已读)
    @RequireLogin
    @GetMapping("/{id}/messages")
    public Result<List<ConsultMessage>> messages(@PathVariable Long id,
                                                 @RequestAttribute("userId") Long userId) {
        return Result.success(consultationService.listUserMessages(userId, id));
    }

    // 发消息
    @RequireLogin
    @PostMapping("/{id}/messages")
    public Result<ConsultMessage> send(@PathVariable Long id,
                                       @RequestBody SendMessageRequest request,
                                       @RequestAttribute("userId") Long userId) {
        return Result.success(consultationService.sendUserMessage(userId, id, request));
    }
}
