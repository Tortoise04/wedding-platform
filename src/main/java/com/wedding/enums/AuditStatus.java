package com.wedding.enums;

// 审核状态 —— 用于商家入驻申请(t_merchant.audit_status)与服务内容审核(t_service.audit_status)。
//
// PENDING 待审核 / APPROVED 已通过 / REJECTED 已驳回。
public enum AuditStatus {

    PENDING("待审核"),
    APPROVED("已通过"),
    REJECTED("已驳回");

    // 中文名,前端展示用
    private final String desc;

    AuditStatus(String desc) {
        this.desc = desc;
    }

    // 取中文名,前端展示用
    public String getDesc() {
        return desc;
    }
}
