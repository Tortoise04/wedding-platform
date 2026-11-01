package com.wedding.dto;

import lombok.Data;

// 管理端 · 隐藏评价的入参。
//
// 隐藏原因必填，超过 200 字在服务端截断。
// ⚠️ 与"驳回"共用 reject_reason 列落库：前端列表里这一列统一叫「驳回原因」，
//    实际可能是"审核驳回原因"，也可能是"通过后人工隐藏原因"——两者都用 REJECTED 表示。
@Data
public class HideEvaluationRequest {

    // 隐藏原因（必填，≤200 字截断）
    private String reason;
}
