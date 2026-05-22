package com.ticket.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类
 *
 * JWT（JSON Web Token）是一种登录凭证
 * 流程：登录成功 → 后端生成一个加密的 token 给前端 → 前端后续请求带上这个 token
 *       → 后端解码 token 就知道是谁在请求（不需要每次都查数据库）
 *
 * token 里包含：用户ID、用户名、角色、过期时间
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expire}")
    private long expire;

    /**
     * 生成 token
     */
    public String generateToken(Long userId, String username, Integer role) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(String.valueOf(userId))           // 用户ID（主要标识）
                .claim("username", username)               // 自定义信息
                .claim("role", role)
                .issuedAt(new Date())                      // 签发时间
                .expiration(new Date(System.currentTimeMillis() + expire * 1000)) // 过期时间
                .signWith(key)
                .compact();
    }

    /**
     * 解析 token，提取 Claims
     *
     * Claims 就是 token 里存的信息（userId, username, role 等）
     * 解析失败（token 过期/伪造）会抛异常
     */
    public Claims parseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 token 中提取用户ID
     */
    public Long getUserId(String token) {
        return Long.parseLong(parseToken(token).getSubject());
    }
}
