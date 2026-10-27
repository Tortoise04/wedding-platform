package com.wedding.service;

import com.wedding.dto.RegisterRequest;
import com.wedding.dto.ResetPasswordRequest;
import com.wedding.dto.UpdateProfileRequest;
import com.wedding.entity.User;

import java.util.List;

// 厨师:用户与登录这道菜。
public interface UserService {

    // 按 id 查用户
    User getUserById(Long id);

    // 按用户名查用户
    User getUserByName(String name);

    // 列出全部用户
    List<User> listUsers();

    // 新建用户
    User createUser(String username, String password, String role);

    // 登录,成功返回 token
    String login(String name,String password);

    // 注册:用户名 + 密码 + 手机号,用户名唯一,新建普通用户
    User register(RegisterRequest request);

    // 密码找回:用户名 + 手机号核对身份后重置密码
    void resetPassword(ResetPasswordRequest request);

    // 编辑个人资料:只改资料字段,用户名 / 密码 / 角色不动
    User updateProfile(Long userId, UpdateProfileRequest request);
}
