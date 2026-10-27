package com.wedding.dto;

import lombok.Data;

// 商家入驻申请请求体:店铺信息 + 资质材料路径。
@Data
public class MerchantApplyRequest {

    // 店铺名称
    private String shopName;

    // 经营范围
    private String businessScope;

    // 联系方式
    private String contactPhone;

    // 店铺简介
    private String description;

    // 店铺 Logo 路径
    private String logo;

    // 营业执照文件路径
    private String licenseUrl;

    // 服务资质证明文件路径
    private String qualificationUrl;
}
