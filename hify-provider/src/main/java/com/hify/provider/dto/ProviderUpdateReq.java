package com.hify.provider.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * 提供商更新请求
 */
@Data
public class ProviderUpdateReq {

    @NotNull(message = "ID 不能为空")
    private Long id;

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
