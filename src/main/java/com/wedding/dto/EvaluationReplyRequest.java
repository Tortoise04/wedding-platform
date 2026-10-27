package com.wedding.dto;

import lombok.Data;

// 商家回复评价的请求体(FR-M-04-4):只有回复内容,商家身份由服务端从令牌推导。
@Data
public class EvaluationReplyRequest {

    // 回复内容(非空)
    private String content;
}
