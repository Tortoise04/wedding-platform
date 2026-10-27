package com.wedding.controller;

import com.wedding.annotation.RequireMerchant;
import com.wedding.common.Result;
import com.wedding.dto.EvaluationReplyRequest;
import com.wedding.entity.Evaluation;
import com.wedding.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 服务员:商家端 · 店铺评价(路径 /api/merchant/evaluations,均 @RequireMerchant)。
@RestController
@RequestMapping("/api/merchant/evaluations")
@RequiredArgsConstructor
public class MerchantEvaluationController {

    private final EvaluationService evaluationService;

    // 本店铺评价(已通过审核的)
    @RequireMerchant
    @GetMapping
    public Result<List<Evaluation>> list(@RequestAttribute("userId") Long userId) {
        return Result.success(evaluationService.listMerchantEvaluations(userId));
    }

    // 回复评价(公开回复,只能回本店已过审的评价)
    @RequireMerchant
    @PostMapping("/{id}/reply")
    public Result<Evaluation> reply(@PathVariable Long id,
                                    @RequestBody EvaluationReplyRequest request,
                                    @RequestAttribute("userId") Long userId) {
        return Result.success(evaluationService.replyEvaluation(userId, id, request));
    }
}
