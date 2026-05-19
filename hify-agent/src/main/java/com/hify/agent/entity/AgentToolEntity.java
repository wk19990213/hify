package com.hify.agent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hify.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Agent 工具绑定实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_tool")
public class AgentToolEntity extends BaseEntity {

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
