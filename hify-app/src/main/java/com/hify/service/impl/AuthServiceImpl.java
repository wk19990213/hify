package com.hify.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hify.common.entity.UserEntity;
import com.hify.common.mapper.UserMapper;
import com.hify.dto.LoginReq;
import com.hify.dto.LoginResp;
import com.hify.dto.RegisterReq;
import com.hify.security.JwtUtils;
import com.hify.service.AuthService;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证服务实现 — 使用 JwtUtils + PasswordEncoder + UserMapper。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;

    @Override
    public LoginResp login(LoginReq req) {
        // 1. 查询用户
        UserEntity user = userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getUsername, req.getUsername())
        );
        if (user == null) {
            log.warn("登录失败：用户名不存在, username={}", req.getUsername());
            throw new RuntimeException("用户名或密码错误");
        }

        // 2. 检查状态
        if (user.getStatus() == null || user.getStatus() != 1) {
            log.warn("登录失败：用户已禁用, username={}", req.getUsername());
            throw new RuntimeException("用户已被禁用，请联系管理员");
        }

        // 3. 验证密码
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            log.warn("登录失败：密码错误, username={}", req.getUsername());
            throw new RuntimeException("用户名或密码错误");
        }

        // 4. 生成 JWT
        String token = jwtUtils.generateToken(user.getId(), user.getUsername());

        // 5. 构建响应
        LoginResp resp = new LoginResp();
        resp.setToken(token);
        resp.setUserId(user.getId());
        resp.setUsername(user.getUsername());
        log.info("用户登录成功: userId={}, username={}", user.getId(), user.getUsername());
        return resp;
    }

    @Override
    public LoginResp register(RegisterReq req) {
        // 1. 检查用户名唯一
        UserEntity existing = userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getUsername, req.getUsername())
        );
        if (existing != null) {
            log.warn("注册失败：用户名已存在, username={}", req.getUsername());
            throw new RuntimeException("用户名已存在");
        }

        // 2. 密码加密
        String hash = passwordEncoder.encode(req.getPassword());

        // 3. 插入用户
        UserEntity user = new UserEntity();
        user.setUsername(req.getUsername());
        user.setPasswordHash(hash);
        user.setDisplayName(req.getDisplayName() != null ? req.getDisplayName() : req.getUsername());
        user.setStatus(1);
        userMapper.insert(user);

        // 4. 生成 JWT
        String token = jwtUtils.generateToken(user.getId(), user.getUsername());

        // 5. 构建响应
        LoginResp resp = new LoginResp();
        resp.setToken(token);
        resp.setUserId(user.getId());
        resp.setUsername(user.getUsername());
        log.info("用户注册成功: userId={}, username={}", user.getId(), user.getUsername());
        return resp;
    }

    @Override
    public LoginResp refresh(String token) {
        Long userId;
        String username;

        try {
            userId = jwtUtils.getUserId(token);
            username = jwtUtils.getUsername(token);
        } catch (ExpiredJwtException e) {
            // token 过期仍允许刷新
            userId = e.getClaims().get("userId", Long.class);
            username = e.getClaims().getSubject();
        } catch (Exception e) {
            log.warn("Token 刷新失败：无效 token, error={}", e.getMessage());
            throw new RuntimeException("无效的 Token");
        }

        // 生成新 token
        String newToken = jwtUtils.generateToken(userId, username);

        LoginResp resp = new LoginResp();
        resp.setToken(newToken);
        resp.setUserId(userId);
        resp.setUsername(username);
        log.info("Token 刷新成功: userId={}, username={}", userId, username);
        return resp;
    }
}
