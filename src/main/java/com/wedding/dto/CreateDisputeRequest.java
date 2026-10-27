package com.wedding.dto;

import lombok.Data;

// 用户发起平台介入的请求体(①申请)。
@Data
public class CreateDisputeRequest {
    // 要发起争议的订单 id
    private Long orderId;
    // 争议诉求说明
    private String reason;
    // 首发附带的一张图片路径(可选,先调 /api/files/upload 拿 url)
    private String fileUrl;
}
