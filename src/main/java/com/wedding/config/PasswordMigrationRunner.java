package com.wedding.config;

import com.wedding.entity.User;
import com.wedding.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// 启动迁移:把库里历史明文密码一次性升级成 BCrypt,幂等,重复启动不会二次加密。
@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordMigrationRunner implements ApplicationRunner {

    // 已是哈希的前缀:不先判断这个,重启就会把 $2a$ 开头的串当明文再哈希一遍
    private static final String BCRYPT_PREFIX = "$2";

    // 用户名仓储:全量扫描 + 就地保存
    private final UserRepository userRepository;
    // 明文转哈希用
    private final PasswordEncoder passwordEncoder;

    // 遍历全部用户,明文覆盖成哈希,最后打一行迁移条数
    @Override
    public void run(ApplicationArguments args) {
        int migrated = 0;
        int skipped = 0;
        for (User user : userRepository.findAll()) {
            String stored = user.getPassword();
            // 空密码没法加密,跳过并计数,不让一条脏数据把启动搞崩
            if (stored == null || stored.isBlank()) {
                skipped++;
                continue;
            }
            // 已是 BCrypt 的直接跳过,这就是幂等的依据
            if (!stored.startsWith(BCRYPT_PREFIX)) {
                user.setPassword(passwordEncoder.encode(stored));
                userRepository.save(user);
                migrated++;
            }
        }
        log.info("明文密码迁移完成: {} 条", migrated);
        if (skipped > 0) {
            log.warn("跳过空密码用户: {} 条", skipped);
        }
    }
}
