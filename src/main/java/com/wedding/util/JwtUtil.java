package com.wedding.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

// JWT 工具类:负责"发卡"与"验卡"两件事。
//
// 通用小工具,不含业务逻辑:generateToken 生成并签名,parseToken 验签并取出载荷。
@Component
public class JwtUtil {

    // token 有效期:2 小时(过期即作废,需重新登录)
    private static final long EXPIRE_MS = 2 * 60 * 60 * 1000L;

    // HS256 要求密钥不短于 32 字节,短了 jjwt 会直接拒绝签字
    private static final int MIN_SECRET_BYTES = 32;

    // 由密钥派生出的签名 / 验签 Key。密钥从配置读(app.jwt.secret / JWT_SECRET),不入源码。
    private final SecretKey key;

    // 构造时把配置里的字符串口径统一成 SecretKey:
    // 配了就用它;没配就随机生成一把(重启后旧 token 全部失效,只适合本地演示)。
    public JwtUtil(@Value("${app.jwt.secret:}") String secret) {
        if (secret == null || secret.isBlank()) {
            this.key = Jwts.SIG.HS256.key().build();
            return;
        }
        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
        if (raw.length < MIN_SECRET_BYTES) {
            // 与其让 jjwt 抛个 WeakKeyException,不如在这里说清楚该改哪里
            throw new IllegalStateException(
                    "app.jwt.secret 至少需要 " + MIN_SECRET_BYTES + " 字节(当前 " + raw.length + " 字节)");
        }
        this.key = Keys.hmacShaKeyFor(raw);
    }

    // 登录成功后签发 token:把 userId 与 role 写进载荷,并用密钥签名以防篡改。
    public String generateToken(Long userId, String role) {
        return Jwts.builder()
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRE_MS))
                .signWith(key)
                .compact();
    }

    // 校验并解析 token;被篡改 / 过期 / 格式不对都会抛异常,由调用方 try-catch 处理。
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
