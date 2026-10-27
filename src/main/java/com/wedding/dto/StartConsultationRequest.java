package com.wedding.dto;

import lombok.Data;

// 发起咨询的请求体(FR-U-04-1):带上要咨询的商家,可指明针对哪个服务。
//
// ⚠️ 不含身份字段 —— 发起人由服务端从令牌推导,不能由前端指定。
@Data
public class StartConsultationRequest {

    // 被咨询商家,关联 t_merchant.id
    private Long merchantId;

    // 咨询针对的服务(可选:不填表示直接咨询店铺)
    private Long serviceId;
}
