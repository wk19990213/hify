package com.hify.provider.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 提供商-模型关联
 */
@Data
@TableName("provider_model")
public class ProviderModelEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long providerId;

    private String modelId;

    private LocalDateTime createdAt;
}
