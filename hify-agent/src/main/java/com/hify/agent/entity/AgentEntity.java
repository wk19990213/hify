package com.hify.agent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hify.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * Agent 配置实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent")
public class AgentEntity extends BaseEntity {

    /**
     * Agent 名称
     */
    private String name;

    /**
     * 唯一编码
     */
    private String code;

    /**
     * 描述
     */
    private String description;

    /**
     * 模型配置 ID
     */
    private Long modelConfigId;

    /**
     * 系统提示词
     */
    private String systemPrompt;

    /**
     * 最大对话轮数
     */
    private Integer conversationMaxRounds;

    /**
     * 温度参数
     */
    private BigDecimal temperature;

    /**
     * 状态：0=禁用，1=启用
     */
    private Long kbId;

    private Integer status;

    /**
     * 排序
     */
    private Integer sortOrder;
}
