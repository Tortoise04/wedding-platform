package com.wedding.dto;

import lombok.Data;

// 管理端 · 发布公告的入参。
//
// ⚠️ 无 publisherId:发布人只能从 token 取,放进请求体前端就能冒名发布。
// ⚠️ 无 publishTime:发布时间由服务端在"发布"那刻盖章,客户端传的时间不可信。
@Data
public class CreateAnnouncementRequest {

    // 公告标题(必填)
    private String title;

    // 公告正文
    private String content;

    // 1 直接发布 / 0 存草稿(不传按 0 处理)
    private Integer status;
}
