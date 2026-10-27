package com.wedding.dto;

import lombok.Data;

// 商家维护案例素材的请求体(FR-M-02-6):一组案例 = 图片 + 标题 + 说明 + 排序。
@Data
public class CaseCreateRequest {

    // 案例图片路径(必填)
    private String imageUrl;

    // 案例视频路径(可选,只接 mp4/webm/mov)
    private String videoUrl;

    // 案例标题
    private String title;

    // 案例说明
    private String description;

    // 案例分类:场布 / 仪式 / 宴席 / 外景 / 其他,可为空
    private String category;

    // 展示排序,数值越小越靠前(缺省 0)
    private Integer sortOrder;
}
