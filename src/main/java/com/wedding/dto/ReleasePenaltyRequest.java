package com.wedding.dto;

import lombok.Data;

// 管理端解除处罚的请求体(FR-A-01-4)。
//
// ⚠️ 与 CreatePenaltyRequest 同一套路:只放业务输入,不放身份字段 ——
// "谁解的"(release_operator_id)由服务端从 token 取,前端传的不可信。
@Data
public class ReleasePenaltyRequest {

    // 解除原因(可空):留痕用,超过 200 字截断
    private String reason;
}
