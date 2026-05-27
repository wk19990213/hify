package com.hify.common.controller;

import com.hify.common.dto.LoginRequest;
import com.hify.common.dto.LoginResponse;
import com.hify.common.dto.RegisterRequest;
import com.hify.common.result.Result;
import com.hify.common.service.AuthService;
import com.hify.common.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest req) {
        return Result.ok(authService.register(req));
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return Result.ok(authService.login(req));
    }

    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(@RequestHeader("Authorization") String auth) {
        String token = auth.startsWith("Bearer ") ? auth.substring(7) : auth;
        Long userId = JwtUtil.getUserId(token);
        return Result.ok(authService.refresh(userId));
    }
}
