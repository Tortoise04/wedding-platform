package com.wedding.controller;

import com.wedding.annotation.RequireAdmin;
import com.wedding.annotation.RequireLogin;
import com.wedding.annotation.RequireMerchant;
import com.wedding.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 四级权限自检接口 —— 阶段 1「登录认证 + 四级权限」的验收演示用。
//
// 依次访问 guest / login / merchant / admin 四个接口,即可把四档权限走一遍。
// ⚠️ 这四个接口只为验收演示存在,阶段 4 有真实业务接口后可整体删除,不影响功能。
@RestController
@RequestMapping("/api/permission-check")
public class PermissionCheckController {

    // 0 档:什么都不贴 → 游客可访问(公开内容,例如浏览服务)
    @GetMapping("/guest")
    public Result<String> guest() {
        return Result.success("通过:0 档接口,没贴标签,游客可访问");
    }

    // 1 档:必须登录
    @RequireLogin
    @GetMapping("/login")
    public Result<String> login() {
        return Result.success("通过:1 档接口,已登录即可访问");
    }

    // 2 档:必须商家(管理员等级更高,也能进)
    @RequireMerchant
    @GetMapping("/merchant")
    public Result<String> merchant() {
        return Result.success("通过:2 档接口,需要商家身份");
    }

    // 3 档:必须管理员
    @RequireAdmin
    @GetMapping("/admin")
    public Result<String> admin() {
        return Result.success("通过:3 档接口,需要管理员身份");
    }
}
