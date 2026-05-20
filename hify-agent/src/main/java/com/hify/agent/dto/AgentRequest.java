package com.hify.agent.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Agent 请求（创建 / 更新共用）
 */
@Data
public class AgentRequest {

    /**
     * Agent 名称
     */
    private String name;

    /**
     * 唯一编码（创建时必填，更新时可选）
     */
    private String code;

    /**
     * 描述
     */
    private String description;

    /**
     * 模型配置 ID（必选）
     */
    @NotNull(message = "必须选择模型配置")
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
     * 温度参数（0-2）
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

    /**
     * 绑定工具列表
     */
    private List<AgentToolRequest> tools;
}
