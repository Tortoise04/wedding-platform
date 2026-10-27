package com.wedding.dto;

import lombok.Data;

// 管理端 · 审核评价的入参。
//
// 字段名沿用商家入驻审核那一套(approved + rejectReason),前后端不用记两种说法。
//
// ⚠️ 这里【没有】auditStatus 字段 —— 状态由 approved 换算(通过 → APPROVED、驳回 → REJECTED)。
//    如果让前端直接传状态,就会出现"approve=true 却把 status 写成 REJECTED"这种自相矛盾的请求,
//    服务端还得再写一段代码去判"到底谁说了算"。**能用一个字段表达清楚的,就不要开两个口子。**
//
// ⚠️ 也【没有】审核人字段:与商家入驻审核保持一致,只记"什么时候审的",不记"谁审的"
//    (这是当前实现的已知限制,已写入文档)。
@Data
public class EvaluationAuditRequest {

    // true 通过(该评价公开展示) / false 驳回(不对外展示)
    private Boolean approved;

    // 驳回原因(驳回时必填;通过时忽略)
    private String rejectReason;
}
