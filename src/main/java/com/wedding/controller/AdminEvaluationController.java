package com.wedding.controller;

import com.wedding.annotation.RequireAdmin;
import com.wedding.common.Result;
import com.wedding.dto.EvaluationAuditRequest;
import com.wedding.entity.Evaluation;
import com.wedding.enums.AuditStatus;
import com.wedding.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 服务员:管理端 · 评价审核(需求 FR-A-04-1)。
//
// 两个接口:
//   1. 看列表 —— status 不传就是全部,前端默认查 PENDING(待办视角)
//   2. 审核单条 —— 通过则公开展示,驳回则不展示
//
// ⚠️ 全部 @RequireAdmin:审核是平台治理动作,用户和商家都碰不得。
//    与用户端那个 /api/evaluations(只贴 @RequireLogin,谁登录都能提交评价)形成对照 ——
//    【谁都能写,但只有管理员能放行】,这是内容治理的常见分工。
@RestController
@RequestMapping("/api/admin/evaluations")
@RequiredArgsConstructor
public class AdminEvaluationController {

    private final EvaluationService evaluationService;

    // 评价列表:auditStatus 不传 = 待审 / 通过 / 驳回全要
    // (传 "PENDING" 就是"待我处理的";传非法值会由全局异常处理器接成 400)
    @RequireAdmin
    @GetMapping
    public Result<List<Evaluation>> list(@RequestParam(required = false) AuditStatus auditStatus) {
        return Result.success(evaluationService.listForAdmin(auditStatus));
    }

    // 审核单条评价:通过(公开展示)/ 驳回(不展示,原因必填)
    @RequireAdmin
    @PostMapping("/{id}/audit")
    public Result<Evaluation> audit(@PathVariable Long id,
                                    @RequestBody EvaluationAuditRequest request) {
        return Result.success(evaluationService.auditEvaluation(id, request));
    }
}
