package com.hify.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hify.agent.dto.AgentListParams;
import com.hify.agent.dto.AgentRequest;
import com.hify.agent.dto.AgentResponse;
import com.hify.agent.dto.AgentToolRequest;
import com.hify.agent.dto.AgentToolResponse;
import com.hify.agent.entity.AgentEntity;
import com.hify.agent.entity.AgentMcpServerEntity;
import com.hify.agent.entity.AgentToolEntity;
import com.hify.agent.mapper.AgentMapper;
import com.hify.agent.mapper.AgentMcpServerMapper;
import com.hify.agent.mapper.AgentToolMapper;
import com.hify.agent.service.AgentService;
import com.hify.common.exception.BizException;
import com.hify.common.result.PageResult;
import com.hify.common.util.PageHelper;
import com.hify.mcp.entity.McpServerEntity;
import com.hify.mcp.mapper.McpServerMapper;
import com.hify.provider.entity.ModelConfigEntity;
import com.hify.provider.mapper.ModelConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Agent 服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

    private static final int DEFAULT_CONVERSATION_ROUNDS = 20;
    private static final BigDecimal DEFAULT_TEMPERATURE = new BigDecimal("0.70");
    private static final int DEFAULT_STATUS = 1;

    private final AgentMapper agentMapper;
    private final AgentToolMapper agentToolMapper;
    private final AgentMcpServerMapper agentMcpServerMapper;
    private final ModelConfigMapper modelConfigMapper;
    private final McpServerMapper mcpServerMapper;

    @Override
    @Transactional
    public Long create(AgentRequest req) {
        // 检查 code 是否已存在
        if (req.getCode() != null && !req.getCode().isBlank()) {
            AgentEntity exist = agentMapper.selectOne(
                    new LambdaQueryWrapper<AgentEntity>()
                            .eq(AgentEntity::getCode, req.getCode())
                            .eq(AgentEntity::getDeleted, 0)
            );
            if (exist != null) {
                throw BizException.paramError("Agent 编码已存在");
            }
        }

        // 创建 Agent 实体
        AgentEntity entity = new AgentEntity();
        BeanUtils.copyProperties(req, entity);

        // 设置默认值
        if (entity.getCode() == null || entity.getCode().isBlank()) {
            entity.setCode("agent-" + java.util.UUID.randomUUID().toString().substring(0, 8));
        }
        if (entity.getConversationMaxRounds() == null) {
            entity.setConversationMaxRounds(DEFAULT_CONVERSATION_ROUNDS);
        }
        if (entity.getTemperature() == null) {
            entity.setTemperature(DEFAULT_TEMPERATURE);
        }
        if (entity.getStatus() == null) {
            entity.setStatus(DEFAULT_STATUS);
        }
        if (entity.getSortOrder() == null) {
            entity.setSortOrder(0);
        }

        agentMapper.insert(entity);

        // 保存工具绑定（旧表 agent_tool）
        if (req.getTools() != null && !req.getTools().isEmpty()) {
            saveAgentTools(entity.getId(), req.getTools());
        }

        // 保存 MCP 服务绑定（新表 agent_mcp_server）
        if (req.getMcpServerIds() != null && !req.getMcpServerIds().isEmpty()) {
            saveAgentMcpServers(entity.getId(), req.getMcpServerIds());
        }

        log.info("Agent created: id={}, name={}, code={}", entity.getId(), entity.getName(), entity.getCode());
        return entity.getId();
    }

    @Override
    @Transactional
    public void update(Long id, AgentRequest req) {
        AgentEntity entity = agentMapper.selectById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw BizException.notFound("Agent 不存在");
        }

        // 检查 code 是否冲突
        if (req.getCode() != null && !req.getCode().isBlank() && !req.getCode().equals(entity.getCode())) {
            AgentEntity exist = agentMapper.selectOne(
                    new LambdaQueryWrapper<AgentEntity>()
                            .eq(AgentEntity::getCode, req.getCode())
                            .eq(AgentEntity::getDeleted, 0)
            );
            if (exist != null) {
                throw BizException.paramError("Agent 编码已存在");
            }
        }

        // 更新字段
        Optional.ofNullable(req.getName()).ifPresent(entity::setName);
        Optional.ofNullable(req.getCode()).ifPresent(entity::setCode);
        Optional.ofNullable(req.getDescription()).ifPresent(entity::setDescription);
        Optional.ofNullable(req.getModelConfigId()).ifPresent(entity::setModelConfigId);
        Optional.ofNullable(req.getKbId()).ifPresent(entity::setKbId);
        Optional.ofNullable(req.getWorkflowId()).ifPresent(entity::setWorkflowId);
        Optional.ofNullable(req.getSystemPrompt()).ifPresent(entity::setSystemPrompt);
        Optional.ofNullable(req.getConversationMaxRounds()).ifPresent(entity::setConversationMaxRounds);
        Optional.ofNullable(req.getTemperature()).ifPresent(entity::setTemperature);
        Optional.ofNullable(req.getStatus()).ifPresent(entity::setStatus);
        Optional.ofNullable(req.getSortOrder()).ifPresent(entity::setSortOrder);

        agentMapper.updateById(entity);

        // 更新工具绑定（先删除后插入）
        if (req.getTools() != null) {
            agentToolMapper.delete(
                    new LambdaQueryWrapper<AgentToolEntity>()
                            .eq(AgentToolEntity::getAgentId, id)
            );
            if (!req.getTools().isEmpty()) {
                saveAgentTools(id, req.getTools());
            }
        }

        // 更新 MCP 服务绑定（新表 agent_mcp_server）
        if (req.getMcpServerIds() != null) {
            // 先软删除所有当前绑定，然后重新插入或激活新绑定
            agentMcpServerMapper.softDeleteByAgentId(id);
            if (!req.getMcpServerIds().isEmpty()) {
                saveAgentMcpServers(id, req.getMcpServerIds());
            }
        }

        log.info("Agent updated: id={}, name={}", id, entity.getName());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        AgentEntity entity = agentMapper.selectById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw BizException.notFound("Agent 不存在");
        }

        // 逻辑删除 Agent
        agentMapper.deleteById(id);

        // 删除工具绑定
        agentToolMapper.delete(
                new LambdaQueryWrapper<AgentToolEntity>()
                        .eq(AgentToolEntity::getAgentId, id)
        );
        agentMcpServerMapper.softDeleteByAgentId(id);

        log.info("Agent deleted: id={}, name={}", id, entity.getName());
    }

    @Override
    public PageResult<AgentResponse> list(AgentListParams params) {
        var pageParam = PageHelper.<AgentEntity>toPage(params.getPage(), params.getPageSize());
        var wrapper = buildQueryWrapper(params);
        var pageResult = agentMapper.selectPage(pageParam, wrapper);

        List<AgentResponse> responses = pageResult.getRecords().stream()
                .map(this::convertToResponse)
                .toList();

        enrichAgentResponses(responses);

        return PageResult.ok(
                responses,
                pageResult.getTotal(),
                pageResult.getCurrent(),
                pageResult.getSize()
        );
    }

    /** 批量填充关联字段：modelConfigName + toolCount + mcpServerIds */
    private void enrichAgentResponses(List<AgentResponse> responses) {
        List<Long> agentIds = responses.stream()
                .map(AgentResponse::getId)
                .toList();
        if (agentIds.isEmpty()) return;

        // 填充 modelConfigName
        List<Long> modelConfigIds = responses.stream()
                .map(AgentResponse::getModelConfigId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (!modelConfigIds.isEmpty()) {
            var modelNameMap = modelConfigMapper.selectList(
                            new LambdaQueryWrapper<ModelConfigEntity>()
                                    .in(ModelConfigEntity::getId, modelConfigIds)
                                    .select(ModelConfigEntity::getId, ModelConfigEntity::getName))
                    .stream()
                    .collect(Collectors.toMap(ModelConfigEntity::getId, ModelConfigEntity::getName, (a, b) -> a));
            for (AgentResponse resp : responses) {
                if (resp.getModelConfigId() != null) {
                    resp.setModelConfigName(modelNameMap.get(resp.getModelConfigId()));
                }
            }
        }

        // 统计工具数量（agent_tool + agent_mcp_server）
        var toolCountMap = agentToolMapper.selectList(
                        new LambdaQueryWrapper<AgentToolEntity>()
                                .in(AgentToolEntity::getAgentId, agentIds))
                .stream()
                .collect(Collectors.groupingBy(AgentToolEntity::getAgentId, Collectors.counting()));

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
            long oldCount = toolCountMap.getOrDefault(resp.getId(), 0L);
            long newCount = mcpServerCountMap.getOrDefault(resp.getId(), 0L);
            resp.setToolCount((int) (oldCount + newCount));
            resp.setMcpServerIds(mcpServerIdsMap.getOrDefault(resp.getId(), List.of()));
        }
    }

    @Override
    public AgentResponse getDetail(Long id) {
        AgentEntity entity = agentMapper.selectById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw BizException.notFound("Agent 不存在");
        }

        AgentResponse resp = convertToResponse(entity);

        // 查询工具列表（旧表 agent_tool）
        List<AgentToolEntity> toolEntities = agentToolMapper.selectList(
                new LambdaQueryWrapper<AgentToolEntity>()
                        .eq(AgentToolEntity::getAgentId, id)
                        .orderByAsc(AgentToolEntity::getSortOrder)
        );

        // 批量填充 MCP 服务名称（旧表）
        List<Long> oldMcpServerIds = toolEntities.stream()
                .map(AgentToolEntity::getMcpServerId)
                .filter(mid -> mid != null)
                .distinct()
                .toList();
        Map<Long, String> mcpNameMap = getMcpNameMap(oldMcpServerIds);

        List<AgentToolResponse> tools = toolEntities.stream()
                .map(t -> {
                    AgentToolResponse tr = convertToolToResponse(t);
                    if (t.getMcpServerId() != null) {
                        tr.setMcpServerName(mcpNameMap.get(t.getMcpServerId()));
                    }
                    return tr;
                })
                .toList();

        resp.setTools(tools);

        // 查询 MCP 服务绑定（新表 agent_mcp_server）
        List<AgentMcpServerEntity> mcpServers = agentMcpServerMapper.selectList(
                new LambdaQueryWrapper<AgentMcpServerEntity>()
                        .eq(AgentMcpServerEntity::getAgentId, id)
                        .orderByAsc(AgentMcpServerEntity::getSortOrder)
        );
        resp.setMcpServerIds(mcpServers.stream()
                .map(AgentMcpServerEntity::getMcpServerId)
                .toList());
        resp.setToolCount(tools.size() + mcpServers.size());
        return resp;
    }

    @Override
    public List<AgentToolResponse> getAgentTools(Long agentId) {
        AgentEntity entity = agentMapper.selectById(agentId);
        if (entity == null || entity.getDeleted() == 1) {
            throw BizException.notFound("Agent 不存在");
        }

        return agentToolMapper.selectList(
                        new LambdaQueryWrapper<AgentToolEntity>()
                                .eq(AgentToolEntity::getAgentId, agentId)
                                .orderByAsc(AgentToolEntity::getSortOrder))
                .stream()
                .map(this::convertToolToResponse)
                .toList();
    }

    @Override
    public List<Long> getAgentMcpServerIds(Long agentId) {
        return agentMcpServerMapper.selectList(
                        new LambdaQueryWrapper<AgentMcpServerEntity>()
                                .eq(AgentMcpServerEntity::getAgentId, agentId)
                                .orderByAsc(AgentMcpServerEntity::getSortOrder))
                .stream()
                .map(AgentMcpServerEntity::getMcpServerId)
                .toList();
    }

    @Override
    @Transactional
    public void batchUpdateStatus(List<Long> ids, Integer status) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        for (Long id : ids) {
            AgentEntity entity = agentMapper.selectById(id);
            if (entity != null && entity.getDeleted() == 0) {
                entity.setStatus(status);
                agentMapper.updateById(entity);
            }
        }

        log.info("Agent batch status updated: ids={}, status={}", ids, status);
    }

    // ========== Private Methods ==========

    private LambdaQueryWrapper<AgentEntity> buildQueryWrapper(AgentListParams params) {
        var wrapper = new LambdaQueryWrapper<AgentEntity>()
                .eq(AgentEntity::getDeleted, 0);

        if (params.getName() != null && !params.getName().isBlank()) {
            wrapper.like(AgentEntity::getName, params.getName());
        }
        if (params.getStatus() != null) {
            wrapper.eq(AgentEntity::getStatus, params.getStatus());
        }
        if (params.getModelConfigId() != null) {
            wrapper.eq(AgentEntity::getModelConfigId, params.getModelConfigId());
        }

        // 排序
        String sortField = params.getSortField();
        String sortOrder = params.getSortOrder();
        if ("sortOrder".equals(sortField)) {
            if ("desc".equalsIgnoreCase(sortOrder)) {
                wrapper.orderByDesc(AgentEntity::getSortOrder);
            } else {
                wrapper.orderByAsc(AgentEntity::getSortOrder);
            }
        } else if ("createdAt".equals(sortField)) {
            if ("asc".equalsIgnoreCase(sortOrder)) {
                wrapper.orderByAsc(AgentEntity::getCreatedAt);
            } else {
                wrapper.orderByDesc(AgentEntity::getCreatedAt);
            }
        } else {
            // 默认排序
            wrapper.orderByAsc(AgentEntity::getSortOrder)
                    .orderByDesc(AgentEntity::getCreatedAt);
        }

        return wrapper;
    }

    private void saveAgentMcpServers(Long agentId, List<Long> mcpServerIds) {
        int order = 0;
        for (Long serverId : mcpServerIds) {
            agentMcpServerMapper.insertOrReactivate(agentId, serverId, order++);
        }
    }

    private Map<Long, String> getMcpNameMap(List<Long> serverIds) {
        if (serverIds.isEmpty()) return Collections.emptyMap();
        return mcpServerMapper.selectList(
                        new LambdaQueryWrapper<McpServerEntity>()
                                .in(McpServerEntity::getId, serverIds)
                                .select(McpServerEntity::getId, McpServerEntity::getName))
                .stream()
                .collect(Collectors.toMap(McpServerEntity::getId, McpServerEntity::getName, (a, b) -> a));
    }

    private void saveAgentTools(Long agentId, List<AgentToolRequest> tools) {
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

    private AgentResponse convertToResponse(AgentEntity entity) {
        AgentResponse resp = new AgentResponse();
        BeanUtils.copyProperties(entity, resp);
        return resp;
    }

    private AgentToolResponse convertToolToResponse(AgentToolEntity entity) {
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
}
