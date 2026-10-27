package com.wedding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// 应用入口:启动 Spring Boot,装配各层 Bean 并开始监听请求。
@SpringBootApplication
public class WeddingPlatformApplication {

    // 启动 Spring Boot 应用(程序总入口)
    public static void main(String[] args) {
        SpringApplication.run(WeddingPlatformApplication.class, args);
    }
}
