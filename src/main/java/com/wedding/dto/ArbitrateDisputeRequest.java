package com.wedding.dto;

import com.wedding.enums.DisputeResultType;
import com.wedding.enums.PenaltyType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// 争议裁决请求体(④裁决)。
//
// 用枚举 resultType 分流四条岔路:REFUND 退款 / CLOSE_ORDER 关闭订单 / REJECT 驳回诉求 / PENALTY 处罚商家。
// 前两者联动取消订单,REJECT 订单不动,PENALTY 订单不动但另插一条 t_penalty;三个 *_penalty 字段仅 PENALTY 有意义。
@Data
public class ArbitrateDisputeRequest {

    // 裁决结论(REFUND / CLOSE_ORDER / REJECT / PENALTY)
    private DisputeResultType resultType;

    // 退款金额(resultType = REFUND 时必填,写入 t_dispute.refund_amount)
    private BigDecimal refundAmount;

    // 裁决说明(给双方看的理由)
    private String result;

    // ======================== 以下仅 resultType = PENALTY 时使用 ========================

    // 处罚类型(WARNING / SERVICE_OFF / BAN),裁决为处罚时必填
    private PenaltyType penaltyType;

    // 处罚对象:仅"SERVICE_OFF 服务下架"时必填,指向 t_service.id
    private Long targetId;

    // 处罚结束时间:可为空,为空表示长期有效(如封禁)
    private LocalDateTime penaltyEndTime;
}
