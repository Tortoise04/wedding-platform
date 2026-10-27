package com.wedding.enums;

// 处罚类型 —— 与 t_penalty.penalty_type 一致(FR-A-01-4:警告 / 服务下架 / 封禁)。
//
// WARNING 警告:口头/书面警告,不影响店铺经营。
// SERVICE_OFF 服务下架:强制下架某个违规服务,需 target_id 指向具体服务;BAN 封禁:封禁店铺,不能再接单。
public enum PenaltyType {

    WARNING("警告"),
    SERVICE_OFF("服务下架"),
    BAN("封禁");

    // 中文名,前端展示用
    private final String desc;

    PenaltyType(String desc) {
        this.desc = desc;
    }

    // 取中文名,前端展示用
    public String getDesc() {
        return desc;
    }
}
