package com.wedding.dto;

import lombok.Data;

// 评价请求体:订单 id + 评分 + 文字评价。
@Data
public class EvaluationRequest {

    // 要评价的订单 id(必须是本人已完成订单)
    private Long orderId;

    // 评分,1 ~ 5
    private Integer score;

    // 文字评价(可选)
    private String content;

    // 评价图片路径,多个以逗号分隔(可选)
    private String images;
}
