package com.hify.agent.dto;

import lombok.Data;

/**
 * Agent 工具绑定响应
 */
@Data
public class AgentToolResponse {

    /**
     * ID
     */
    private Long id;

    /**
     * Agent ID
     */
    private Long agentId;

    /**
     * 工具名称
     */
    private String toolName;

    /**
     * 工具类型：mcp/builtin
     */
    private String toolType;

    /**
     * MCP 服务 ID
     */
    private Long mcpServerId;

    /**
     * MCP 服务名称
     */
    private String mcpServerName;

    /**
     * 工具配置 JSON
     */
    private String configJson;

    /**
     * 排序
     */
    private Integer sortOrder;
}
