package com.wedding.enums;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;

// 订单状态机 —— 五个稳定状态。
//
// 五态:PENDING 待审核 / CONFIRMED 已确认 / IN_PROGRESS 履约中 / COMPLETED 已完成 / CANCELLED 已取消。
// 库中以 @Enumerated(EnumType.STRING) 存枚举名,与《数据库设计说明书》3.5 节 t_order.status 取值一致;用枚举而非魔法字符串,编译期即校验、可用 == 比较。
@Getter
public enum OrderStatus {

    PENDING("待审核"),
    CONFIRMED("已确认"),
    IN_PROGRESS("履约中"),
    COMPLETED("已完成"),
    CANCELLED("已取消");

    // 中文名,前端展示用
    private final String desc;

    OrderStatus(String desc) {
        this.desc = desc;
    }

}
