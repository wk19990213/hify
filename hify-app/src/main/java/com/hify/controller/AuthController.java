package com.hify.controller;

import com.hify.common.result.Result;
import com.hify.dto.LoginReq;
import com.hify.dto.LoginResp;
import com.hify.dto.RegisterReq;
import com.hify.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器 — 登录/注册/Token 刷新。
 * 路径白名单已在 SecurityConfig 中配置。
 */
@Slf4j
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录。
     */
    @PostMapping("/login")
    public Result<LoginResp> login(@Valid @RequestBody LoginReq req) {
        LoginResp resp = authService.login(req);
        return Result.ok(resp);
    }

    /**
     * 用户注册。
     */
    @PostMapping("/register")
    public Result<LoginResp> register(@Valid @RequestBody RegisterReq req) {
        LoginResp resp = authService.register(req);
        return Result.ok(resp);
    }

    /**
     * 刷新 Token。
     */
    @PostMapping("/refresh")
    public Result<LoginResp> refresh(@RequestBody String token) {
        LoginResp resp = authService.refresh(token);
        return Result.ok(resp);
    }
}
