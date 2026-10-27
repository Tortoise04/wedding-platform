package com.wedding.dto;

import com.wedding.enums.PenaltyType;
import lombok.Data;

import java.time.LocalDateTime;

// 管理端执行处罚的请求体(FR-A-01-4 / FR-A-03-5)。
//
// ⚠️ 只放业务输入,不放身份字段 ——"谁罚的"(operator_id)一律由服务端从 token 取,前端传的不可信。
@Data
public class CreatePenaltyRequest {

    // 被处罚的店铺,指向 t_merchant.id
    private Long merchantId;

    // 处罚类型:WARNING 警告 / SERVICE_OFF 服务下架 / BAN 封禁
    private PenaltyType penaltyType;

    // 处罚对象:仅 SERVICE_OFF 时必填,指向要被下架的 t_service.id;其余类型留空
    private Long targetId;

    // 处罚原因(必填)
    private String reason;

    // 处罚结束时间;为空表示长期有效(如封禁)
    private LocalDateTime endTime;
}
