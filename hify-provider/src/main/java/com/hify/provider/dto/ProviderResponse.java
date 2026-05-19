package com.hify.provider.dto;

import com.hify.provider.entity.ModelConfigEntity;
import com.hify.provider.entity.ProviderHealthEntity;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 提供商响应（列表 / 详情共用）
 */
@Data
public class ProviderResponse {

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

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /** 模型配置列表（仅详情接口填充） */
    private List<ModelConfigEntity> modelConfigs;

    /** 已启用模型数量（列表接口填充） */
    private int modelCount;

    /** 健康状态（详情/列表均可填充） */
    private ProviderHealthEntity health;
}
