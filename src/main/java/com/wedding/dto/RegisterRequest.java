package com.wedding.dto;

import lombok.Data;

// 用户注册的请求体(FR-U-01-1):用户名 + 密码 + 手机号必填,真实姓名 / 邮箱选填。
@Data
public class RegisterRequest {

    // 登录名(全局唯一)
    private String username;

    // 密码
    private String password;

    // 手机号
    private String phone;

    // 真实姓名(选填)
    private String realName;

    // 邮箱(选填)
    private String email;
}
