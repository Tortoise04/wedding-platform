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

    // 咨询来源订单(可选:从「我的订单」点「咨询商家」时带上)
    // 服务端会核实这单确实是「我」在「这家店」下的,核不过直接 400 —— 否则谁都能把别人的订单 id 塞进来。
    private Long orderId;
}
