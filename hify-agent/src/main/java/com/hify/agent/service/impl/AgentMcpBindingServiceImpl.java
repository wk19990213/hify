package com.hify.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hify.agent.dto.AgentResponse;
import com.hify.agent.entity.AgentMcpServerEntity;
import com.hify.agent.mapper.AgentMcpServerMapper;
import com.hify.agent.service.AgentMcpBindingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Agent MCP 服务绑定服务实现（新表 agent_mcp_server）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentMcpBindingServiceImpl implements AgentMcpBindingService {

    private final AgentMcpServerMapper agentMcpServerMapper;

    @Override
    public void bindMcpServers(Long agentId, List<Long> mcpServerIds) {
        int order = 0;
        for (Long serverId : mcpServerIds) {
            agentMcpServerMapper.insertOrReactivate(agentId, serverId, order++);
        }
    }

    @Override
    public void unbindMcpServers(Long agentId) {
        agentMcpServerMapper.softDeleteByAgentId(agentId);
    }

    @Override
    public List<Long> getBoundMcpServerIds(Long agentId) {
        return agentMcpServerMapper.selectList(
                        new LambdaQueryWrapper<AgentMcpServerEntity>()
                                .eq(AgentMcpServerEntity::getAgentId, agentId)
                                .orderByAsc(AgentMcpServerEntity::getSortOrder))
                .stream()
                .map(AgentMcpServerEntity::getMcpServerId)
                .toList();
    }

    @Override
    public void enrichMcpBindings(List<AgentResponse> responses, List<Long> agentIds) {
        var mcpServerRecords = agentMcpServerMapper.selectList(
                new LambdaQueryWrapper<AgentMcpServerEntity>()
                        .in(AgentMcpServerEntity::getAgentId, agentIds));
        var mcpServerCountMap = mcpServerRecords.stream()
                .collect(Collectors.groupingBy(AgentMcpServerEntity::getAgentId, Collectors.counting()));
        var mcpServerIdsMap = mcpServerRecords.stream()
                .collect(Collectors.groupingBy(
                        AgentMcpServerEntity::getAgentId,
                        Collectors.mapping(AgentMcpServerEntity::getMcpServerId, Collectors.toList())));
        for (AgentResponse resp : responses) {
            long oldCount = resp.getToolCount();
            long newCount = mcpServerCountMap.getOrDefault(resp.getId(), 0L);
            resp.setToolCount((int) (oldCount + newCount));
            resp.setMcpServerIds(mcpServerIdsMap.getOrDefault(resp.getId(), List.of()));
        }
    }
}
