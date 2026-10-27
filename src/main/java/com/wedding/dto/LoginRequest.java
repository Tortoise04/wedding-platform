package com.wedding.dto;

import lombok.Data;

// 登录请求体:密码放 body 走,不再拼进 URL 查询串,避免密码落进访问日志与浏览器历史。
@Data
public class LoginRequest {

    // 登录名
    private String name;

    // 明文密码,只在服务端比对时用
    private String password;
}
