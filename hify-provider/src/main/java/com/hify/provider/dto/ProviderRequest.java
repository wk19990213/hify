package com.hify.provider.dto;

import lombok.Data;

import java.util.Map;

/**
 * 提供商请求（创建 / 更新共用）
 */
@Data
public class ProviderRequest {

    private String name;

    private String code;

    private String type;

    private String baseUrl;

    private Map<String, Object> authConfig;

    private Integer timeoutMs;

    private Integer maxRetries;

    private Integer retryIntervalMs;

    private Integer status;

    private Integer sortOrder;

    private Map<String, Object> extraConfig;
}
