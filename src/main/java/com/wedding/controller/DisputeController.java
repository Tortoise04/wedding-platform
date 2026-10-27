package com.wedding.controller;

import com.wedding.annotation.RequireLogin;
import com.wedding.common.Result;
import com.wedding.dto.CreateDisputeRequest;
import com.wedding.dto.SubmitEvidenceRequest;
import com.wedding.entity.Dispute;
import com.wedding.entity.DisputeEvidence;
import com.wedding.service.DisputeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 服务员:用户端 · 争议与举证(需求 FR-A-03,路径 /api/disputes,均 @RequireLogin)。
@RestController
@RequestMapping("/api/disputes")
@RequiredArgsConstructor
public class DisputeController {
    private final DisputeService disputeService;

    // 我的争议列表(用户端,按发起时间倒序)
    @RequireLogin
    @GetMapping
    public Result<List<Dispute>> listMyDisputes(@RequestAttribute("userId") Long userId) {
        return Result.success(disputeService.listMyDisputes(userId));
    }

    // 当事人查看争议详情(仅发起人与被投诉店铺店主可看)
    @RequireLogin
    @GetMapping("/{id}")
    public Result<Dispute> getMyDispute(@PathVariable Long id,
                                        @RequestAttribute("userId") Long userId) {
        return Result.success(disputeService.getMyDispute(id, userId));
    }

    // 当事人查看已提交的全部举证材料(按提交时间正序,前端当聊天流用)
    @RequireLogin
    @GetMapping("/{id}/evidences")
    public Result<List<DisputeEvidence>> listMyEvidences(@PathVariable Long id,
                                                         @RequestAttribute("userId") Long userId) {
        return Result.success(disputeService.listMyEvidences(id, userId));
    }

    // 用户对某订单发起平台介入(创建争议)
    @RequireLogin
    @PostMapping
    public Result<Dispute> create(@RequestBody CreateDisputeRequest request,
                                  @RequestAttribute("userId") Long userId) {
        return Result.success(disputeService.createDispute(userId, request));
    }

    // 双方举证:提交一份举证材料(用户与商家共用此接口)。
    // 谁是提交方由服务端根据当前登录人判定,请求体里【没有】身份字段。
    @RequireLogin
    @PostMapping("/{id}/evidence")
    public Result<DisputeEvidence> submitEvidence(@PathVariable Long id,
                                                  @RequestBody SubmitEvidenceRequest request,
                                                  @RequestAttribute("userId") Long userId) {
        return Result.success(disputeService.submitEvidence(id, userId, request));
    }
}
