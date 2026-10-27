package com.wedding.dto;

import lombok.Data;

// 编辑个人资料的请求体(FR-U-01-4):只改资料字段,用户名 / 密码 / 角色一律不可改。
@Data
public class UpdateProfileRequest {

    // 真实姓名
    private String realName;

    // 手机号
    private String phone;

    // 邮箱
    private String email;

    // 头像文件路径
    private String avatar;
}
