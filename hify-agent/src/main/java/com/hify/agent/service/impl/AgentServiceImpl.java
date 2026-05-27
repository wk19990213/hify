package com.hify.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hify.agent.dto.AgentListParams;
import com.hify.agent.dto.AgentRequest;
import com.hify.agent.dto.AgentResponse;
import com.hify.agent.dto.AgentToolResponse;
import com.hify.agent.entity.AgentEntity;
import com.hify.agent.mapper.AgentConvertMapper;
import com.hify.agent.mapper.AgentMapper;
import com.hify.agent.service.AgentMcpBindingService;
import com.hify.agent.service.AgentService;
import com.hify.agent.service.AgentToolService;
import com.hify.common.exception.BizException;
import com.hify.common.result.PageResult;
import com.hify.common.util.PageHelper;
import com.hify.provider.entity.ModelConfigEntity;
import com.hify.provider.mapper.ModelConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

    private static final int DEFAULT_CONVERSATION_ROUNDS = 20;
    private static final BigDecimal DEFAULT_TEMPERATURE = new BigDecimal("0.70");
    private static final int DEFAULT_STATUS = 1;

    private final AgentMapper agentMapper;
    private final ModelConfigMapper modelConfigMapper;
    private final AgentToolService agentToolService;
    private final AgentMcpBindingService agentMcpBindingService;

    @Override
    @Transactional
    public Long create(AgentRequest req) {
        if (req.getCode() != null && !req.getCode().isBlank()) {
            AgentEntity exist = agentMapper.selectOne(
                    new LambdaQueryWrapper<AgentEntity>()
                            .eq(AgentEntity::getCode, req.getCode())
                            .eq(AgentEntity::getDeleted, 0));
            if (exist != null) {
                throw BizException.paramError("Agent 编码已存在");
            }
        }
        AgentEntity entity = new AgentEntity();
        BeanUtils.copyProperties(req, entity);
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
        if (req.getTools() != null && !req.getTools().isEmpty()) {
            agentToolService.bindTools(entity.getId(), req.getTools());
        }
        if (req.getMcpServerIds() != null && !req.getMcpServerIds().isEmpty()) {
            agentMcpBindingService.bindMcpServers(entity.getId(), req.getMcpServerIds());
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
        if (req.getCode() != null && !req.getCode().isBlank() && !req.getCode().equals(entity.getCode())) {
            AgentEntity exist = agentMapper.selectOne(
                    new LambdaQueryWrapper<AgentEntity>()
                            .eq(AgentEntity::getCode, req.getCode())
                            .eq(AgentEntity::getDeleted, 0));
            if (exist != null) {
                throw BizException.paramError("Agent 编码已存在");
            }
        }
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
        if (req.getTools() != null) {
            agentToolService.unbindTools(id);
            if (!req.getTools().isEmpty()) {
                agentToolService.bindTools(id, req.getTools());
            }
        }
        if (req.getMcpServerIds() != null) {
            agentMcpBindingService.unbindMcpServers(id);
            if (!req.getMcpServerIds().isEmpty()) {
                agentMcpBindingService.bindMcpServers(id, req.getMcpServerIds());
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
        agentMapper.deleteById(id);
        agentToolService.unbindTools(id);
        agentMcpBindingService.unbindMcpServers(id);
        log.info("Agent deleted: id={}, name={}", id, entity.getName());
    }
    @Override
    public PageResult<AgentResponse> list(AgentListParams params) {
        var pageParam = PageHelper.<AgentEntity>toPage(params.getPage(), params.getPageSize());
        var wrapper = buildQueryWrapper(params);
        var pageResult = agentMapper.selectPage(pageParam, wrapper);
        List<AgentResponse> responses = pageResult.getRecords().stream()
                .map(this::convertToResponse).toList();
        List<Long> agentIds = responses.stream().map(AgentResponse::getId).toList();
        if (!agentIds.isEmpty()) {
            enrichModelNames(responses, agentIds);
            agentToolService.enrichToolCounts(responses, agentIds);
            agentMcpBindingService.enrichMcpBindings(responses, agentIds);
        }
        return PageResult.ok(responses, pageResult.getTotal(), pageResult.getCurrent(), pageResult.getSize());
    }
    private void enrichModelNames(List<AgentResponse> responses, List<Long> agentIds) {
        List<Long> modelConfigIds = responses.stream()
                .map(AgentResponse::getModelConfigId).filter(id -> id != null).distinct().toList();
        if (modelConfigIds.isEmpty()) return;
        var modelNameMap = modelConfigMapper.selectList(
                        new LambdaQueryWrapper<ModelConfigEntity>()
                                .in(ModelConfigEntity::getId, modelConfigIds)
                                .select(ModelConfigEntity::getId, ModelConfigEntity::getName))
                .stream().collect(Collectors.toMap(ModelConfigEntity::getId, ModelConfigEntity::getName, (a, b) -> a));
        for (AgentResponse resp : responses) {
            if (resp.getModelConfigId() != null) {
                resp.setModelConfigName(modelNameMap.get(resp.getModelConfigId()));
            }
        }
    }
    @Override
    public AgentResponse getDetail(Long id) {
        AgentEntity entity = agentMapper.selectById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw BizException.notFound("Agent 不存在");
        }
        AgentResponse resp = convertToResponse(entity);
        List<AgentToolResponse> tools = agentToolService.getAgentTools(id);
        List<Long> oldMcpServerIds = tools.stream()
                .map(AgentToolResponse::getMcpServerId).filter(mid -> mid != null).distinct().toList();
        var mcpNameMap = agentToolService.getMcpNameMap(oldMcpServerIds);
        for (AgentToolResponse tr : tools) {
            if (tr.getMcpServerId() != null) {
                tr.setMcpServerName(mcpNameMap.get(tr.getMcpServerId()));
            }
        }
        resp.setTools(tools);
        List<Long> mcpServerIds = agentMcpBindingService.getBoundMcpServerIds(id);
        resp.setMcpServerIds(mcpServerIds);
        resp.setToolCount(tools.size() + mcpServerIds.size());
        return resp;
    }
    @Override
    public List<AgentToolResponse> getAgentTools(Long agentId) {
        AgentEntity entity = agentMapper.selectById(agentId);
        if (entity == null || entity.getDeleted() == 1) {
            throw BizException.notFound("Agent 不存在");
        }
        return agentToolService.getAgentTools(agentId);
    }
    @Override
    public List<Long> getAgentMcpServerIds(Long agentId) {
        return agentMcpBindingService.getBoundMcpServerIds(agentId);
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
    private LambdaQueryWrapper<AgentEntity> buildQueryWrapper(AgentListParams params) {
        var wrapper = new LambdaQueryWrapper<AgentEntity>().eq(AgentEntity::getDeleted, 0);
        if (params.getName() != null && !params.getName().isBlank()) {
            wrapper.like(AgentEntity::getName, params.getName());
        }
        if (params.getStatus() != null) {
            wrapper.eq(AgentEntity::getStatus, params.getStatus());
        }
        if (params.getModelConfigId() != null) {
            wrapper.eq(AgentEntity::getModelConfigId, params.getModelConfigId());
        }
        String sf = params.getSortField();
        String so = params.getSortOrder();
        if ("sortOrder".equals(sf)) {
            if ("desc".equalsIgnoreCase(so)) {
                wrapper.orderByDesc(AgentEntity::getSortOrder);
            } else {
                wrapper.orderByAsc(AgentEntity::getSortOrder);
            }
        } else if ("createdAt".equals(sf)) {
            if ("asc".equalsIgnoreCase(so)) {
                wrapper.orderByAsc(AgentEntity::getCreatedAt);
            } else {
                wrapper.orderByDesc(AgentEntity::getCreatedAt);
            }
        } else {
            wrapper.orderByAsc(AgentEntity::getSortOrder).orderByDesc(AgentEntity::getCreatedAt);
        }
        return wrapper;
    }

    private AgentResponse convertToResponse(AgentEntity entity) {
        return AgentConvertMapper.INSTANCE.toResponse(entity);
    }
}
