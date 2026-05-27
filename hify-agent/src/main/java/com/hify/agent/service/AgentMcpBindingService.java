package com.hify.agent.service;

import com.hify.agent.dto.AgentResponse;

import java.util.List;

/**
 * Agent MCP 服务绑定服务（新表 agent_mcp_server）
 */
public interface AgentMcpBindingService {

    /**
     * 批量绑定 MCP 服务
     */
    void bindMcpServers(Long agentId, List<Long> mcpServerIds);

    /**
     * 删除所有 MCP 服务绑定（软删除）
     */
    void unbindMcpServers(Long agentId);

    /**
     * 获取绑定的 MCP 服务 ID 列表
     */
    List<Long> getBoundMcpServerIds(Long agentId);

    /**
     * 批量填充 MCP 绑定信息（含数量和 ID 列表）
     */
    void enrichMcpBindings(List<AgentResponse> responses, List<Long> agentIds);
}
