package com.hify.provider.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hify.common.entity.BaseEntity;
import com.hify.common.handler.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 模型配置实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "model_config", autoResultMap = true)
public class ModelConfigEntity extends BaseEntity {

    /** 所属提供商 ID */
    private Long providerId;

    /** 原始模型标识，如 gpt-4-turbo */
    private String modelId;

    /** 显示名称，如 GPT-4 Turbo */
    private String name;

    /** 唯一编码，如 gpt4t */
    private String code;

    /** 能力配置 JSON */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object capabilities;

    /** 计费配置 JSON */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object priceConfig;

    /** 提供此模型的供应商数量 */
    private Integer providerCount;

    /** 状态：0=禁用 1=启用 2=deprecated */
    private Integer status;

    /** 是否默认模型 */
    private Integer isDefault;

    /** 排序 */
    private Integer sortOrder;
}
