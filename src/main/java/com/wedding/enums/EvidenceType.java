package com.wedding.enums;

// 举证材料类型 —— 与 t_dispute_evidence.evidence_type 一致。
public enum EvidenceType {

    IMAGE("图片"),
    TEXT("文字"),
    // 文字与图片同时提交时的组合类型
    MIXED("图文");

    // 中文名,前端展示用
    private final String desc;

    EvidenceType(String desc) {
        this.desc = desc;
    }

    // 取中文名,前端展示用
    public String getDesc() {
        return desc;
    }
}
