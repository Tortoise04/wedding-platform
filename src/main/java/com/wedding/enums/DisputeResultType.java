package com.wedding.enums;

// 争议裁决结论 —— 与 t_dispute.result_type 一致。
public enum DisputeResultType {

    REFUND("退款"),
    CLOSE_ORDER("关闭订单"),
    PENALTY("处罚商家"),
    REJECT("驳回诉求");

    // 中文名,前端展示用
    private final String desc;

    DisputeResultType(String desc) {
        this.desc = desc;
    }

    // 取中文名,前端展示用
    public String getDesc() {
        return desc;
    }
}
