package com.wedding.dto;

import lombok.Data;

// 商家维护店铺信息的请求体(FR-M-01-5):只改简介 / 联系方式 / 经营范围 / Logo。
//
// ⚠️ 店铺名称、资质材料、审核状态、账号状态都不在这里 —— 那些要么入驻时定死、要么归管理员管。
@Data
public class UpdateMerchantRequest {

    // 经营范围
    private String businessScope;

    // 联系方式
    private String contactPhone;

    // 店铺简介
    private String description;

    // 店铺 Logo 路径
    private String logo;
}
