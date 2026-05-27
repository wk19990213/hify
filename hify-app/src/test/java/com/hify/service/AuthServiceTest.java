package com.hify.service;

import com.hify.common.entity.UserEntity;
import com.hify.common.mapper.UserMapper;
import com.hify.dto.LoginReq;
import com.hify.dto.LoginResp;
import com.hify.dto.RegisterReq;
import com.hify.security.JwtUtils;
import com.hify.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * T017: AuthService 单元测试 — 登录/注册/刷新 Token。
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserMapper userMapper;

    private AuthService authService;
    private JwtUtils jwtUtils;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() throws Exception {
        jwtUtils = new JwtUtils();
        setField(jwtUtils, "secret", "test-secret-key-for-jwt-which-must-be-at-least-256-bits-long!!");
        setField(jwtUtils, "expiration", 3600000L); // 1 hour

        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthServiceImpl(userMapper, jwtUtils, passwordEncoder);
    }

    // ======================== login ========================

    @Test
    void testLoginSuccess() {
        String rawPassword = "password123";
        String hash = passwordEncoder.encode(rawPassword);
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPasswordHash(hash);
        user.setStatus(1);

        when(userMapper.selectOne(any())).thenReturn(user);

        LoginReq req = new LoginReq();
        req.setUsername("testuser");
        req.setPassword(rawPassword);

        LoginResp resp = authService.login(req);

        assertNotNull(resp);
        assertEquals(1L, resp.getUserId());
        assertEquals("testuser", resp.getUsername());
        assertNotNull(resp.getToken());
        assertFalse(resp.getToken().isEmpty());
    }

    @Test
    void testLoginWrongPassword() {
        String hash = passwordEncoder.encode("correctPassword");
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPasswordHash(hash);
        user.setStatus(1);

        when(userMapper.selectOne(any())).thenReturn(user);

        LoginReq req = new LoginReq();
        req.setUsername("testuser");
        req.setPassword("wrongPassword");

        assertThrows(RuntimeException.class, () -> authService.login(req));
    }

    @Test
    void testLoginUserNotFound() {
        when(userMapper.selectOne(any())).thenReturn(null);

        LoginReq req = new LoginReq();
        req.setUsername("nonexistent");
        req.setPassword("password");

        assertThrows(RuntimeException.class, () -> authService.login(req));
    }

    @Test
    void testLoginUserDisabled() {
        String rawPassword = "password123";
        String hash = passwordEncoder.encode(rawPassword);
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setUsername("disabledUser");
        user.setPasswordHash(hash);
        user.setStatus(0); // 禁用

        when(userMapper.selectOne(any())).thenReturn(user);

        LoginReq req = new LoginReq();
        req.setUsername("disabledUser");
        req.setPassword(rawPassword);

        assertThrows(RuntimeException.class, () -> authService.login(req));
    }

    // ======================== register ========================

    @Test
    void testRegisterSuccess() {
        when(userMapper.selectOne(any())).thenReturn(null);
        // 模拟数据库自增 ID 赋值
        doAnswer(invocation -> {
            UserEntity entity = invocation.getArgument(0);
            entity.setId(100L);
            return 1;
        }).when(userMapper).insert(any(UserEntity.class));

        RegisterReq req = new RegisterReq();
        req.setUsername("newuser");
        req.setPassword("password123");
        req.setDisplayName("New User");

        LoginResp resp = authService.register(req);

        assertNotNull(resp);
        assertEquals("newuser", resp.getUsername());
        assertEquals(100L, resp.getUserId());
        assertNotNull(resp.getToken());
        assertFalse(resp.getToken().isEmpty());
    }

    @Test
    void testRegisterDuplicateUsername() {
        UserEntity existing = new UserEntity();
        existing.setId(1L);
        existing.setUsername("existing");

        when(userMapper.selectOne(any())).thenReturn(existing);

        RegisterReq req = new RegisterReq();
        req.setUsername("existing");
        req.setPassword("password123");

        assertThrows(RuntimeException.class, () -> authService.register(req));
    }

    // ======================== refresh ========================

    @Test
    void testRefreshTokenSuccess() {
        String oldToken = jwtUtils.generateToken(1L, "testuser");

        LoginResp resp = authService.refresh(oldToken);

        assertNotNull(resp);
        assertEquals(1L, resp.getUserId());
        assertEquals("testuser", resp.getUsername());
        assertNotNull(resp.getToken());
        assertFalse(resp.getToken().isEmpty());
        // 验证新 token 可解析
        assertEquals(1L, jwtUtils.getUserId(resp.getToken()));
        assertEquals("testuser", jwtUtils.getUsername(resp.getToken()));
    }

    @Test
    void testRefreshTokenInvalidToken() {
        assertThrows(RuntimeException.class, () -> authService.refresh("invalid.token.here"));
    }

    // ======================== helper ========================

    private void setField(Object obj, String fieldName, Object value) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }
}
