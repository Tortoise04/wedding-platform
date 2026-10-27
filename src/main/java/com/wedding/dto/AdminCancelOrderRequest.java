package com.wedding.dto;

import lombok.Data;

// 管理端 · 强制取消订单的请求体(FR-A-02 履约监督 / 异常处置)。
//
// ⚠️ 只放"管理员能决定的业务数据",不放管理员 id —— 操作人身份一律由服务端从 token 取,前端传什么身份都不可信。
@Data
public class AdminCancelOrderRequest {

    // 取消原因 —— 必填。管理端介入属强制动作,必须向用户/商家说清依据,故非空校验
    private String reason;
}
