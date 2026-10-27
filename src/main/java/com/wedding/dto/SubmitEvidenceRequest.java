package com.wedding.dto;

import lombok.Data;

// 争议举证提交请求体(③双方举证)。
//
// ⚠️ 用户与商家共用,且没有 submitterRole ——"我是哪一方"不能由前端说了算(会被伪造),必须服务端按登录人判出。
// ⚠️ 也刻意没有 evidenceType:材料类型由服务端按 content/fileUrl 是否为空推导,前端传什么都不作数。
@Data
public class SubmitEvidenceRequest {

    // 文字说明,选填;有内容则材料类型至少是 TEXT
    private String content;

    // 证明材料地址(/uploads/... 上传返回的 url),选填;有则材料类型至少是 IMAGE
    private String fileUrl;
}
