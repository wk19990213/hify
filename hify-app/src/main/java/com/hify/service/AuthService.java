package com.hify.service;

import com.hify.dto.LoginReq;
import com.hify.dto.LoginResp;
import com.hify.dto.RegisterReq;

/**
 * 认证服务接口 — 登录/注册/Token 刷新。
 */
public interface AuthService {

    /**
     * 登录：验证用户名密码，返回 JWT Token。
     *
     * @param req 登录请求
     * @return 包含 token 的登录响应
     * @throws RuntimeException 用户名不存在或密码错误时抛出
     */
    LoginResp login(LoginReq req);

    /**
     * 注册：创建新用户，返回 JWT Token。
     *
     * @param req 注册请求
     * @return 包含 token 的登录响应
     * @throws RuntimeException 用户名已存在时抛出
     */
    LoginResp register(RegisterReq req);

    /**
     * 刷新 Token：基于旧 token 中的用户信息生成新 token。
     * 旧 token 过期也允许刷新。
     *
     * @param token 旧 JWT token
     * @return 包含新 token 的登录响应
     * @throws RuntimeException token 无效时抛出
     */
    LoginResp refresh(String token);
}
