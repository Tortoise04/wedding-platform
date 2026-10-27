package com.wedding.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// 密码编码配置:只注册一个 BCrypt 编码器,不引 security starter,认证仍由 JwtInterceptor 负责。
@Configuration
public class PasswordConfig {

    // BCrypt 每次编码带随机盐,同一明文结果不同,所以校验只能走 matches 不能用 equals
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
