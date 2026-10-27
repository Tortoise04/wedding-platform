package com.wedding.controller;

import com.wedding.annotation.RequireAdmin;
import com.wedding.annotation.RequireLogin;
import com.wedding.common.Result;
import com.wedding.dto.LoginRequest;
import com.wedding.dto.RegisterRequest;
import com.wedding.dto.ResetPasswordRequest;
import com.wedding.dto.UpdateProfileRequest;
import com.wedding.entity.User;
import com.wedding.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 服务员:用户端 · 用户与登录(路径 /api/users)。
//
// 权限档位:不贴 → 游客可访问(login 必须保持不贴,否则谁都登不进来);@RequireLogin → 登录;
// @RequireMerchant → 商家;@RequireAdmin → 管理员。
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // GET /api/users/{id} → 按 id 查一个用户(1 档:登录后即可查看)
    // ⚠️ 注意:本接口会把 real_name / phone / email 一并返回,属于"登录用户可看他人资料",
    //    后续做个人中心时应改成"只能看自己",或返回不含敏感字段的 VO。
    @RequireLogin
    @GetMapping("/{id}")
    public Result<User> getUser(@PathVariable Long id) {
        return Result.success(userService.getUserById(id));
    }

    // GET /api/users/getUserByName/{name} → 按用户名查(3 档:属管理端检索能力)
    @RequireAdmin
    @GetMapping("/getUserByName/{name}")
    public Result<User> getUserByName(@PathVariable String name) {
        return Result.success(userService.getUserByName(name));
    }

    // POST /api/users/login → 登录(0 档:**绝对不能贴权限标签**;密码走请求体,不再出现在 URL 里)
    @PostMapping("/login")
    public Result<String> login(@RequestBody LoginRequest request) {
        return Result.success(userService.login(request.getName(), request.getPassword()));
    }

    // POST /api/users/register → 注册(0 档:游客可注册)
    @PostMapping("/register")
    public Result<User> register(@RequestBody RegisterRequest request) {
        return Result.success(userService.register(request));
    }

    // POST /api/users/reset-password → 密码找回(0 档:未登录也能找回)
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        userService.resetPassword(request);
        return Result.success(null);
    }

    // PUT /api/users/profile → 编辑个人资料(1 档:只能改自己)
    @RequireLogin
    @PutMapping("/profile")
    public Result<User> updateProfile(@RequestBody UpdateProfileRequest request,
                                      @RequestAttribute("userId") Long userId) {
        return Result.success(userService.updateProfile(userId, request));
    }

    // GET /api/users → 列出所有用户(3 档:管理端)
    @RequireAdmin
    @GetMapping
    public Result<List<User>> listUsers() {
        return Result.success(userService.listUsers());
    }
}
