package com.wedding.dto;

import lombok.Data;

// 管理员启用 / 停用商家的请求体。
@Data
public class MerchantStatusRequest {

    // 1 启用 / 0 停用
    private Integer status;

    // 停用原因(停用必填,写进罚单留痕);启用时可空
    private String reason;
}
