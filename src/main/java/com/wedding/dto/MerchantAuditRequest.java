package com.wedding.dto;

import lombok.Data;

// 商家入驻审核请求体。
@Data
public class MerchantAuditRequest {

    // true = 通过;false = 驳回
    private Boolean approved;

    // 驳回原因(驳回时填写)
    private String rejectReason;
}
