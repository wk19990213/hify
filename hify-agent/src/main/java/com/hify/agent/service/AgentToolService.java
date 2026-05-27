package com.hify.agent.service;

import com.hify.agent.dto.AgentToolRequest;
import com.hify.agent.dto.AgentToolResponse;
import com.hify.agent.dto.AgentResponse;
import com.hify.agent.entity.AgentToolEntity;

import java.util.List;
import java.util.Map;

/**
 * Agent 工具绑定服务（旧表 agent_tool，灰度期）
 */
public interface AgentToolService {

    /**
     * 批量绑定工具
     */
    void bindTools(Long agentId, List<AgentToolRequest> tools);

    /**
     * 删除所有工具绑定
     */
    void unbindTools(Long agentId);

    /**
     * 获取 Agent 绑定的工具列表
     */
    List<AgentToolResponse> getAgentTools(Long agentId);

    /**
     * 实体转响应
     */
    AgentToolResponse toResponse(AgentToolEntity entity);

    /**
     * 批量查询 MCP 服务名称
     */
    Map<Long, String> getMcpNameMap(List<Long> serverIds);

    /**
     * 批量填充工具数量
     */
    void enrichToolCounts(List<AgentResponse> responses, List<Long> agentIds);
}
