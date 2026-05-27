package com.hify.common.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hify.common.dto.LoginRequest;
import com.hify.common.dto.LoginResponse;
import com.hify.common.dto.RegisterRequest;
import com.hify.common.entity.UserEntity;
import com.hify.common.exception.BizException;
import com.hify.common.mapper.UserMapper;
import com.hify.common.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public LoginResponse register(RegisterRequest req) {
        try {
            UserEntity exist = userMapper.selectOne(
                    new LambdaQueryWrapper<UserEntity>()
                            .eq(UserEntity::getUsername, req.getUsername())
                            .eq(UserEntity::getDeleted, 0));
            if (exist != null) {
                throw BizException.paramError("用户名已存在");
            }

            UserEntity user = new UserEntity();
            user.setUsername(req.getUsername());
            user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
            user.setDisplayName(req.getDisplayName() != null ? req.getDisplayName() : req.getUsername());
            user.setStatus(1);
            userMapper.insert(user);

            log.info("User registered: id={}, username={}", user.getId(), user.getUsername());
            String token = JwtUtil.generateToken(user.getId(), user.getUsername());
            return new LoginResponse(token, user.getId(), user.getUsername());
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Registration failed for username={}: {}", req.getUsername(), e.getMessage(), e);
            throw new RuntimeException("注册失败: " + e.getMessage(), e);
        }
    }

    public LoginResponse login(LoginRequest req) {
        UserEntity user = userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getUsername, req.getUsername())
                        .eq(UserEntity::getDeleted, 0));
        if (user == null || user.getStatus() == null || user.getStatus() == 0) {
            throw BizException.paramError("用户名或密码错误");
        }
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw BizException.paramError("用户名或密码错误");
        }

        String token = JwtUtil.generateToken(user.getId(), user.getUsername());
        return new LoginResponse(token, user.getId(), user.getUsername());
    }

    public LoginResponse refresh(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() == 1) {
            throw BizException.paramError("用户不存在");
        }
        String token = JwtUtil.generateToken(user.getId(), user.getUsername());
        return new LoginResponse(token, user.getId(), user.getUsername());
    }
}
