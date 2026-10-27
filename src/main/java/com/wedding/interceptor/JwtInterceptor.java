package com.wedding.interceptor;

import com.wedding.annotation.RequireAdmin;
import com.wedding.annotation.RequireLogin;
import com.wedding.annotation.RequireMerchant;
import com.wedding.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.lang.reflect.Method;

// JWT 拦截器:在进入 Controller 前统一做登录与角色权限校验。
//
// 所需等级由方法上的注解决定:RequireLogin=1 / RequireMerchant=2 / RequireAdmin=3,未标注=0(公开)。
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtInterceptor implements HandlerInterceptor {

    // token 签发与解析工具
    private final JwtUtil jwtUtil;

    // 拦截每个 /api/** 请求:判定所需等级 → 取并验签 token → 等级不足则拦下。
    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws IOException {

        // 非 Controller 方法(如静态资源)读不到注解,直接放行
        if (!(handler instanceof HandlerMethod handlerMethod)){
            return true;
        }
        // 取到目标方法,据其注解判定所需权限等级

        Method method = handlerMethod.getMethod();

        // 由方法注解决定所需等级:0 公开 / 1 登录 / 2 商家 / 3 管理员
        int identify =0;
        if (method.getAnnotation(RequireLogin.class) != null) {
            identify = 1;}
        if (method.getAnnotation(RequireMerchant.class) != null) {
            identify = 2;
        }
        if (method.getAnnotation(RequireAdmin.class) != null) {
            identify = 3;
        }

        log.info("{} → 需要等级 = {}", request.getRequestURI(),identify);

        // 从 Authorization 头取 Bearer token
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            // 未带 token 或格式不对
            if (identify == 0) {
                // 公开接口,游客可访问
                return true;
            }
            return reject(response, 401, "请先登录");
        }

        String token = header.substring("Bearer ".length());

        // 验签:通过后把"当前用户是谁"写入 request,供后面的 Controller 取用
        try {
            Claims claims = jwtUtil.parseToken(token);

            Long userId = ((Number) claims.get("userId")).longValue();
            request.setAttribute("userId", userId);

            String role = claims.get("role", String.class);
            // 把角色换算成权限等级
            int userLevel = levelOf(role);

            log.info("{} → 验签通过:role = {},等级 = {}",
                    request.getRequestURI(), role, userLevel);

            // 等级不足即拒绝(身份有效但无权访问)
            if (userLevel < identify) {
                log.warn("{} → 权限不足:role = {} 需要等级 = {}",
                        request.getRequestURI(), role, identify);
                return reject(response, 403, "没有权限访问该功能");
            }

            return true;

        } catch (ExpiredJwtException e) {
            if (identify == 0) {
                // 可选登录:过期也当游客放行
                return true;
            }
            return reject(response, 401, "登录已过期,请重新登录");

        } catch (JwtException | IllegalArgumentException e) {
            if (identify == 0) {
                // 可选登录:解析异常同样放行
                return true;
            }
            return reject(response, 401, "登录凭证无效");
        }
    }

    // 角色换算成权限等级:ADMIN=3 / MERCHANT=2 / USER=1 / 其他=0
    private int levelOf(String role) {
        if (role == null) {
            return 0;  // 默认最低权限
        }
        return switch (role) {
            case "ADMIN" -> 3;
            case "MERCHANT" -> 2;
            case "USER" -> 1;
            default -> 0;
        };
    }

    // 拦下请求:自行写状态码 + JSON。
    // ⚠️ 格式必须和 GlobalExceptionHandler 返回的一致,否则前端要写两套解析逻辑。
    private boolean reject(HttpServletResponse response, int code, String msg) throws IOException {
        response.setStatus(code);                                          // ① 状态码
        response.setContentType("application/json;charset=UTF-8");         // ② 声明 JSON
        response.getWriter().write("{\"code\":" + code + ",\"msg\":\"" + msg + "\",\"data\":null}");   // ③ 写内容
        return false;                                                      // ④ 拒绝!
    }




}
