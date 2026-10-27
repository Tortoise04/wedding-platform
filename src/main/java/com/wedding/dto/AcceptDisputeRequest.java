package com.wedding.dto;

import lombok.Data;

// 争议受理判断请求体(②受理)。
//
// 与商家入驻审核同构:用一个布尔值分两条岔路,而不开两个接口。
@Data
public class AcceptDisputeRequest {

    // true = 受理(→ 举证中);false = 不予受理(→ 已关闭)
    private Boolean approved;

    // 不予受理的原因(approved = false 时填写,写入 t_dispute.result)
    private String rejectReason;
}
