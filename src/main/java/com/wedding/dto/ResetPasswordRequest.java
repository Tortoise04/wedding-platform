package com.wedding.dto;

import lombok.Data;

// 密码找回的请求体(FR-U-01-3):用"用户名 + 手机号"核对身份后重置密码。
//
// ⚠️ 本项目无短信/邮件服务,身份核对简化为"手机号与注册手机号一致"(已写入文档的已知简化)。
@Data
public class ResetPasswordRequest {

    // 登录名
    private String username;

    // 注册手机号(用于核对身份)
    private String phone;

    // 新密码
    private String newPassword;
}
