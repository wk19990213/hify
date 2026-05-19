package com.hify.provider.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * 提供商创建请求
 */
@Data
public class ProviderCreateReq {

    @NotBlank(message = "名称不能为空")
    private String name;

    private String code;

    @NotBlank(message = "类型不能为空")
    private String type;

    @NotBlank(message = "Base URL 不能为空")
    private String baseUrl;

    private Map<String, Object> authConfig;

    private Integer timeoutMs;

    private Integer maxRetries;

    private Integer retryIntervalMs;

    private Integer status;

    private Integer sortOrder;

    private Map<String, Object> extraConfig;
}
