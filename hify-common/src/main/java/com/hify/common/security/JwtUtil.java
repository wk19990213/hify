package com.hify.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类 — 无状态 token 生成与验证
 */
public class JwtUtil {

    private static final String SECRET = System.getProperty("jwt.secret",
            "hify-jwt-secret-key-change-in-production-256bit!!");
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    private static final long EXPIRATION_MS = 24 * 60 * 60 * 1000; // 24h

    public static String generateToken(Long userId, String username, Map<String, Object> extra) {
        var builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS));
        if (extra != null) extra.forEach(builder::claim);
        return builder.signWith(KEY).compact();
    }

    public static String generateToken(Long userId, String username) {
        return generateToken(userId, username, null);
    }

    public static Claims parseToken(String token) {
        return Jwts.parser().verifyWith(KEY).build()
                .parseSignedClaims(token).getPayload();
    }

    public static boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static Long getUserId(String token) {
        return Long.parseLong(parseToken(token).getSubject());
    }
}
