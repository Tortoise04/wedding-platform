package com.wedding.service;

import com.wedding.dto.EvaluationAuditRequest;
import com.wedding.dto.EvaluationReplyRequest;
import com.wedding.dto.EvaluationRequest;
import com.wedding.dto.HideEvaluationRequest;
import com.wedding.entity.Evaluation;
import com.wedding.enums.AuditStatus;

import java.util.Collection;
import java.util.List;
import java.util.Map;

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

    // 【管理端】评价列表,可再按商家收窄(商家详情抽屉的评价 Tab 用);两个条件都不传 = 全部
    List<Evaluation> listForAdmin(AuditStatus auditStatus, Long merchantId);

    // 【管理端】审核一条评价 —— 通过则公开展示,驳回则不展示;adminId 来自 token,用于留痕
    Evaluation auditEvaluation(Long adminId, Long evaluationId, EvaluationAuditRequest request);

    // 【管理端】隐藏一条已通过的评价 —— APPROVED → REJECTED 并置 hidden_at,原因必填(hidden_at 非空 = 人工隐藏)
    // adminId 来自 token,写入 hidden_operator_id(不动 audit_operator_id,审核人保持不变)
    Evaluation hideEvaluation(Long adminId, Long evaluationId, HideEvaluationRequest request);

    // 【管理端】恢复一条已隐藏的评价 —— REJECTED 且 hidden_at 非空 → APPROVED,清空 hidden_at 与隐藏原因
    // adminId 只用于记日志:恢复后 hidden_at / hidden_operator_id 一起清空,"谁恢复的"没有落库位置
    Evaluation restoreEvaluation(Long adminId, Long evaluationId);

    // 【商家端】本店铺评价(已通过审核的,按时间倒序)
    List<Evaluation> listMerchantEvaluations(Long userId);

    // 【商家端】回复评价(只能回本店、且已过审的评价)
    Evaluation replyEvaluation(Long userId, Long evaluationId, EvaluationReplyRequest request);

    // 【用户端】当页订单里哪些已经评价过 —— 返回 {"订单id": true},前端用 evaluated[orderId] 取。
    // 只吃当页的订单 id 集合:集合为空直接返回空表,不发查询。
    Map<String, Boolean> evaluatedOrderIds(Collection<Long> orderIds);
}
