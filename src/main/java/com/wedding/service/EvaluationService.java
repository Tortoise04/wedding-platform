package com.wedding.service;

import com.wedding.dto.EvaluationAuditRequest;
import com.wedding.dto.EvaluationReplyRequest;
import com.wedding.dto.EvaluationRequest;
import com.wedding.entity.Evaluation;
import com.wedding.enums.AuditStatus;

import java.util.List;

// 菜单:评价能做什么。
//
// 本轮给它加了一道"公开闸门"(FR-A-04-1):用户提交的评价不再直接对外可见,
// 而是先落到 PENDING,等管理员放行(APPROVED)后才出现在服务详情页与商家端。
public interface EvaluationService {

    // 用户评价已完成订单(前置:本人订单 + 订单已完成 + 一单一评)。落库为【待审核】。
    Evaluation createEvaluation(Long userId, EvaluationRequest request);

    // 【用户端】我写过的评价(含待审 / 已通过 / 已驳回)—— 前端据此把「评价」按钮换成「已评价」
    List<Evaluation> listMyEvaluations(Long userId);

    // 【管理端】评价列表:auditStatus 传 null 就是"待审 + 通过 + 驳回"全要
    List<Evaluation> listForAdmin(AuditStatus auditStatus);

    // 【管理端】审核一条评价 —— 通过则公开展示,驳回则不展示
    Evaluation auditEvaluation(Long evaluationId, EvaluationAuditRequest request);

    // 【商家端】本店铺评价(已通过审核的,按时间倒序)
    List<Evaluation> listMerchantEvaluations(Long userId);

    // 【商家端】回复评价(只能回本店、且已过审的评价)
    Evaluation replyEvaluation(Long userId, Long evaluationId, EvaluationReplyRequest request);
}
