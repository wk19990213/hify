package com.hify.provider.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 提供商响应
 */
@Data
public class ProviderResp {

    private Long id;

    private String name;

    private String code;

    private String type;

    private String baseUrl;

    private Integer timeoutMs;

    private Integer maxRetries;

    private Integer retryIntervalMs;

    private Integer status;

    private Integer sortOrder;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
