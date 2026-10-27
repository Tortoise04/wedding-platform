package com.wedding.dto;


import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

// 用户下单请求体:选定服务并填写婚礼日期、人数、预算等,生成一笔待接单订单。
@Data
public class CreateOrderRequest {
    // 下单的服务 id,指向 t_service.id
    private Long serviceId;
    // 婚礼举办日期
    private LocalDate weddingDate;
    // 宾客人数
    private Integer guestCount;
    // 预算下限
    private BigDecimal budgetMin;
    // 预算上限
    private BigDecimal budgetMax;
    // 数量(场次/份数)
    private Integer quantity;
    // 备注(可空)
    private String remark;
}
