package com.neuedu.movieapi.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${jwt.secret:defaultSecretKey12345678901234567890123456789012}")
    private String secret;             // 配置中的密钥字符串

    @Value("${jwt.expiration:3600000}")
    private Long expiration;             // 过期时间（毫秒）

    /**
     * 根据密钥字符串生成 HMAC SHA 密钥对象（用于新版 API）
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 JWT Token
     * @param username 用户名（作为 subject）
     * @return JWT 字符串
     */
    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        return Jwts.builder()
                .claims(claims)                      // 设置自定义载荷
                .subject(username)                  // 设置主题
                .issuedAt(new Date())               // 签发时间
                .expiration(new Date(System.currentTimeMillis() + expiration)) // 过期时间
                .signWith(getSigningKey(), Jwts.SIG.HS256)  // 使用 HS256 签名
                .compact();
    }

    /**
     * 解析 JWT Token，返回 Claims（载荷）
     * @param token JWT 字符串
     * @return Claims 对象
     * @throws JwtException 如果签名无效或 Token 过期
     */
    public Claims parseToken(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(getSigningKey())        // 使用新方法验证密钥
                .build()
                .parseSignedClaims(token)           // 解析并验证签名
                .getPayload();                      // 获取载荷
    }

    /**
     * 从 Token 中提取用户名（subject）
     * @param token JWT 字符串
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * 验证 Token 是否有效（签名正确且未过期）
     * @param token JWT 字符串
     * @return true 有效，false 无效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException e) {
            // 签名错误、过期等异常
            return false;
        } catch (Exception e) {
            // 其他意外异常
            return false;
        }
    }
}