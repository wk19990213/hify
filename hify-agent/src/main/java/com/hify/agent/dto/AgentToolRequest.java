package com.hify.agent.dto;

import lombok.Data;

/**
 * Agent 工具绑定请求
 */
@Data
public class AgentToolRequest {

    /**
     * 工具名称
     */
    private String toolName;

    /**
     * 工具类型：mcp/builtin
     */
    private String toolType;

    /**
     * MCP 服务 ID（tool_type=mcp 时）
     */
    private Long mcpServerId;

    /**
     * 工具配置 JSON
     */
    private String configJson;

    /**
     * 排序
     */
    private Integer sortOrder;
}
