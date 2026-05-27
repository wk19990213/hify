package com.hify.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hify.agent.dto.AgentToolRequest;
import com.hify.agent.dto.AgentToolResponse;
import com.hify.agent.dto.AgentResponse;
import com.hify.agent.entity.AgentToolEntity;
import com.hify.agent.mapper.AgentToolMapper;
import com.hify.agent.service.AgentToolService;
import com.hify.mcp.entity.McpServerEntity;
import com.hify.mcp.mapper.McpServerMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Agent 工具绑定服务实现（旧表 agent_tool，灰度期）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentToolServiceImpl implements AgentToolService {

    private final AgentToolMapper agentToolMapper;
    private final McpServerMapper mcpServerMapper;

    @Override
    public void bindTools(Long agentId, List<AgentToolRequest> tools) {
        int order = 0;
        for (AgentToolRequest toolReq : tools) {
            AgentToolEntity toolEntity = new AgentToolEntity();
            toolEntity.setAgentId(agentId);
            toolEntity.setToolName(toolReq.getToolName());
            toolEntity.setToolType(toolReq.getToolType());
            toolEntity.setMcpServerId(toolReq.getMcpServerId());
            toolEntity.setConfigJson(toolReq.getConfigJson());
            toolEntity.setSortOrder(toolReq.getSortOrder() != null ? toolReq.getSortOrder() : order++);
            agentToolMapper.insert(toolEntity);
        }
    }

    @Override
    public void unbindTools(Long agentId) {
        agentToolMapper.delete(
                new LambdaQueryWrapper<AgentToolEntity>()
                        .eq(AgentToolEntity::getAgentId, agentId)
        );
    }

    @Override
    public List<AgentToolResponse> getAgentTools(Long agentId) {
        return agentToolMapper.selectList(
                        new LambdaQueryWrapper<AgentToolEntity>()
                                .eq(AgentToolEntity::getAgentId, agentId)
                                .orderByAsc(AgentToolEntity::getSortOrder))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public AgentToolResponse toResponse(AgentToolEntity entity) {
        AgentToolResponse resp = new AgentToolResponse();
        resp.setId(entity.getId());
        resp.setAgentId(entity.getAgentId());
        resp.setToolName(entity.getToolName());
        resp.setToolType(entity.getToolType());
        resp.setMcpServerId(entity.getMcpServerId());
        resp.setConfigJson(entity.getConfigJson());
        resp.setSortOrder(entity.getSortOrder());
        return resp;
    }

    @Override
    public Map<Long, String> getMcpNameMap(List<Long> serverIds) {
        if (serverIds.isEmpty()) return Collections.emptyMap();
        return mcpServerMapper.selectList(
                        new LambdaQueryWrapper<McpServerEntity>()
                                .in(McpServerEntity::getId, serverIds)
                                .select(McpServerEntity::getId, McpServerEntity::getName))
                .stream()
                .collect(Collectors.toMap(McpServerEntity::getId, McpServerEntity::getName, (a, b) -> a));
    }

    @Override
    public void enrichToolCounts(List<AgentResponse> responses, List<Long> agentIds) {
        var toolCountMap = agentToolMapper.selectList(
                        new LambdaQueryWrapper<AgentToolEntity>()
                                .in(AgentToolEntity::getAgentId, agentIds))
                .stream()
                .collect(Collectors.groupingBy(AgentToolEntity::getAgentId, Collectors.counting()));
        for (AgentResponse resp : responses) {
            resp.setToolCount(toolCountMap.getOrDefault(resp.getId(), 0L).intValue());
        }
    }
}
