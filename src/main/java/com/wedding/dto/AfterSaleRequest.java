package com.wedding.dto;

import lombok.Data;

// 商家端 · 登记售后处理的请求体。
//
// ⚠️ 只放商家能决定的业务数据,不放商家 id / 操作人 —— 身份一律由服务端从 token(@RequestAttribute userId)取。
@Data
public class AfterSaleRequest {

    // 售后状态:1 处理中 / 2 已完结,必填且只能是这两个值
    private Integer status;

    // 售后处理说明,必填,给用户看的
    private String note;
}
