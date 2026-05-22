package com.hify.agent.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Agent 响应（列表 / 详情共用）
 */
@Data
public class AgentResponse {

    /**
     * ID
     */
    private Long id;

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
     * 模型配置名称（列表接口填充）
     */
    private String modelConfigName;

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

    private Long kbId;

    /**
     * 绑定工作流 ID
     */
    private Long workflowId;

    /**
     * 状态：0=禁用，1=启用
     */
    private Integer status;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 绑定工具列表（仅详情接口填充）
     */
    private List<AgentToolResponse> tools;

    /**
     * 工具数量（列表接口填充）
     */
    private int toolCount;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
