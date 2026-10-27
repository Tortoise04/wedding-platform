package com.wedding.config;

import com.wedding.interceptor.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


// Web 配置:把 JWT 拦截器挂到所有 /api/** 接口上。
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {
    // 登录 / 权限校验拦截器
    private final JwtInterceptor jwtInterceptor;

    // 注册拦截器:/api/** 全路径生效,统一在做业务前校验登录与角色权限
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**");// 拦哪些网址?
    }

    // 静态资源:把 /uploads/** 映射到磁盘的 uploads/ 目录,让前端能直接 <img src> 打开上传的图。
    // 位置必须是 file: 开头的绝对路径(不能写死盘符,项目换个盘就跑不了);
    // 结尾那个 / 不能省:少了它 Spring 会把子路径解析到 uploads 的上一级,访问图片全是 404。
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadPath = System.getProperty("user.dir").replace("\\", "/") + "/uploads/";
        registry.addResourceHandler("/uploads/**").addResourceLocations("file:" + uploadPath);
    }
}
