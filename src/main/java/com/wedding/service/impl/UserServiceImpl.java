package com.wedding.service.impl;

import com.wedding.dto.RegisterRequest;
import com.wedding.dto.ResetPasswordRequest;
import com.wedding.dto.UpdateProfileRequest;
import com.wedding.entity.User;
import com.wedding.exception.BusinessException;
import com.wedding.repository.UserRepository;
import com.wedding.service.UserService;
import com.wedding.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 厨师:用户这道菜(查询 / 新增 / 登录),业务逻辑都写在这一层。
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    // 数据访问:用户仓储
    private final UserRepository userRepository;
    // 登录成功后签发 token 用
    private final JwtUtil jwtUtil;
    // 密码加密与校验(BCrypt),注册 / 找回 / 登录都靠它
    private final PasswordEncoder passwordEncoder;

    // 是否已是 BCrypt 哈希:只认 $2 开头,否则会把哈希当明文再加密一次
    private boolean isBcrypt(String stored) {
        return stored != null && stored.startsWith("$2");
    }

    // 按 id 查用户:查不到即报错,供订单 / 权限等复用。
    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在: " + id));
    }

    // 按用户名查用户:登录与注册查重都走这里。
    @Override
    public User getUserByName(String name){
        return userRepository.findByUsername(name)
                .orElseThrow(() -> new BusinessException("用户不存在: " + name));
    }

    // 用户列表:全量返回,仅管理端使用。
    @Override
    public List<User> listUsers() {
        return userRepository.findAll();
    }

    // 新建用户:闸门是用户名非空,role 由调用方传入。
    @Override
    public User createUser(String username, String password, String role) {
        if (username == null || username.isBlank()) {
            throw new BusinessException("用户名不能为空");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setRole(role);
        return userRepository.save(user);
    }

    // 登录:先查用户再校验密码(兼容历史明文),通过后签发 JWT(带 userId 与 role)。
    @Override
    public  String login(String name,String password){
        if (name == null || name.isBlank()) {
            throw new BusinessException("用户名不能为空");
        }
        if (password == null || password.isBlank()) {
            throw new BusinessException("密码不能为空");
        }
        User user = userRepository.findByUsername(name)
                .orElseThrow(() -> new BusinessException("用户不存在: " + name));
        if (!passwordMatches(user, password)){
            throw new BusinessException("密码错误");
        }
        return jwtUtil.generateToken(user.getId(), user.getRole());
    }

    // 密码校验:哈希走 matches,历史明文走 equals 兜底,匹配成功后就地加密存回(惰性升级)。
    private boolean passwordMatches(User user, String raw) {
        String stored = user.getPassword();
        // 请求体没带密码时 raw 为 null,BCrypt 的 matches 会直接抛异常,这里先挡掉
        if (raw == null) {
            return false;
        }
        if (isBcrypt(stored)) {
            return passwordEncoder.matches(raw, stored);
        }
        if (!raw.equals(stored)) {
            return false;
        }
        user.setPassword(passwordEncoder.encode(raw));
        userRepository.save(user);
        return true;
    }

    // 注册:校验必填项 + 用户名查重,新建普通用户(role=USER、status=1)。
    //
    // 密码存库前先 BCrypt 编码,与登录的 matches 校验配套。
    @Transactional
    @Override
    public User register(RegisterRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new BusinessException("用户名不能为空");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BusinessException("密码不能为空");
        }
        if (request.getPhone() == null || request.getPhone().isBlank()) {
            throw new BusinessException("手机号不能为空");
        }
        if (request.getUsername().length() > 50) {
            throw new BusinessException("用户名不能超过 50 个字符");
        }
        if (request.getPassword().length() > 100) {
            throw new BusinessException("密码过长");
        }
        if (request.getPhone().length() > 20) {
            throw new BusinessException("手机号格式不正确");
        }
        if (request.getRealName() != null && request.getRealName().length() > 50) {
            throw new BusinessException("姓名不能超过 50 个字符");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("用户名已存在");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setRealName(request.getRealName());
        user.setEmail(request.getEmail());
        user.setRole("USER");
        user.setStatus(1);
        return userRepository.save(user);
    }

    // 密码找回:用户名定位账号,手机号核对身份,一致才重置密码。
    @Transactional
    @Override
    public void resetPassword(ResetPasswordRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new BusinessException("用户名不能为空");
        }
        if (request.getPhone() == null || request.getPhone().isBlank()) {
            throw new BusinessException("手机号不能为空");
        }
        if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            throw new BusinessException("新密码不能为空");
        }
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("用户不存在: " + request.getUsername()));
        if (!request.getPhone().equals(user.getPhone())) {
            throw new BusinessException("手机号与注册手机号不一致");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    // 编辑个人资料:只覆盖资料字段,用户名 / 密码 / 角色一律不碰。
    @Transactional
    @Override
    public User updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在: " + userId));
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setAvatar(request.getAvatar());
        return userRepository.save(user);
    }
}
