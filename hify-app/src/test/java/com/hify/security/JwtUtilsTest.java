package com.hify.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SEC-01: JwtUtils 测试 — token 生成/解析/过期验证。
 */
class JwtUtilsTest {

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() throws Exception {
        jwtUtils = new JwtUtils();
        // 用反射注入 secret 和 expiration
        setField(jwtUtils, "secret", "test-secret-key-for-jwt-which-must-be-at-least-256-bits-long!!");
        setField(jwtUtils, "expiration", 3600000L); // 1 hour
    }

    @Test
    void testGenerateTokenCreatesValidJwt() {
        String token = jwtUtils.generateToken(1L, "testuser");
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void testParseTokenReturnsUserId() {
        String token = jwtUtils.generateToken(42L, "admin");
        Long userId = jwtUtils.getUserId(token);
        assertEquals(42L, userId);
    }

    @Test
    void testParseTokenReturnsUsername() {
        String token = jwtUtils.generateToken(7L, "alice");
        String username = jwtUtils.getUsername(token);
        assertEquals("alice", username);
    }

    @Test
    void testExpiredTokenThrowsException() throws Exception {
        JwtUtils expiredJwt = new JwtUtils();
        setField(expiredJwt, "secret", "test-secret-key-for-jwt-which-must-be-at-least-256-bits-long!!");
        setField(expiredJwt, "expiration", -1L); // negative = already expired

        String token = expiredJwt.generateToken(1L, "user");
        assertThrows(Exception.class, () -> expiredJwt.getUserId(token));
    }

    @Test
    void testInvalidTokenThrowsException() {
        assertThrows(Exception.class, () -> jwtUtils.getUserId("invalid.token.here"));
    }

    private void setField(Object obj, String fieldName, Object value) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }
}
