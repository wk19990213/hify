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

    /**
     * 所属提供商 ID
     */
    private Long providerId;

    /**
     * 模型名称（原始模型标识，如 gpt-4、claude-3-opus）
     */
    private String modelName;

    /**
     * 展示名称
     */
    private String displayName;

    /**
     * 模型类型：llm/embedding/rerank
     */
    private String modelType;

    /**
     * 上下文窗口大小（token 数）
     */
    private Integer contextWindow;

    /**
     * 最大输出 token 数
     */
    private Integer maxTokens;

    /**
     * 是否支持流式：0=否，1=是
     */
    private Integer isStream;

    /**
     * 扩展配置（JSON）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object configJson;

    /**
     * 是否默认模型
     */
    private Integer isDefault;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 状态：0=禁用，1=启用
     */
    private Integer status;
}
