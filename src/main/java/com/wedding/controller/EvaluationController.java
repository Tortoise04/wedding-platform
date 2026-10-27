package com.wedding.controller;

import com.wedding.annotation.RequireLogin;
import com.wedding.common.Result;
import com.wedding.dto.EvaluationRequest;
import com.wedding.entity.Evaluation;
import com.wedding.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 服务员:用户端 · 订单评价(路径 /api/evaluations,需登录)。
@RestController
@RequestMapping("/api/evaluations")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    // 我写过的评价(含待审 / 已通过 / 已驳回)—— 订单列表据此判断「评价」按钮该不该可点
    @RequireLogin
    @GetMapping("/my")
    public Result<List<Evaluation>> my(@RequestAttribute("userId") Long userId) {
        return Result.success(evaluationService.listMyEvaluations(userId));
    }

    // 评价已完成订单
    @RequireLogin
    @PostMapping
    public Result<Evaluation> create(@RequestBody EvaluationRequest request,
                                     @RequestAttribute("userId") Long userId) {
        return Result.success(evaluationService.createEvaluation(userId, request));
    }
}
