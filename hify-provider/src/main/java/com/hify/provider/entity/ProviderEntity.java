package com.hify.provider.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hify.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 模型提供商实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("provider")
public class ProviderEntity extends BaseEntity {

    private String name;

    private String code;

    private String type;

    private String baseUrl;

    private String authConfig;

    private Integer timeoutMs;

    private Integer maxRetries;

    private Integer retryIntervalMs;

    private Integer status;

    private Integer sortOrder;

    private Object extraConfig;
}
