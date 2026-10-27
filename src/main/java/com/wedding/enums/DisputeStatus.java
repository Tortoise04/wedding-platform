package com.wedding.enums;

// 争议状态机 —— 与 t_dispute.status 一致。
//
// 待受理 → 举证中 → 仲裁中 → 已裁决;不符合受理条件的直接 → 已关闭。
// ⚠️ 与订单状态机是【两套独立】的状态机,互不占用(见《系统设计》4.4)。
public enum DisputeStatus {

    PENDING("待受理"),
    PROCESSING("举证中"),
    ARBITRATING("仲裁中"),
    FINISHED("已裁决"),
    CLOSED("已关闭");

    // 中文名,前端展示用
    private final String desc;

    DisputeStatus(String desc) {
        this.desc = desc;
    }

    // 取中文名,前端展示用
    public String getDesc() {
        return desc;
    }
}
