package com.hify.dto;

import lombok.Data;

/**
 * 登录/注册/刷新 Token 响应 DTO。
 */
@Data
public class LoginResp {

    private String token;
    private Long userId;
    private String username;
}
