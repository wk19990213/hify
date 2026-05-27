package com.hify.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.hify.common.exception.BizException;
import com.hify.common.result.PageResult;
import com.hify.common.util.PageHelper;
import com.hify.workflow.dto.*;
import com.hify.workflow.engine.ExecutionState;
import com.hify.workflow.engine.WorkflowEngine;
import com.hify.workflow.entity.*;
import com.hify.workflow.mapper.*;
import com.hify.workflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowServiceImpl implements WorkflowService {

    private final WorkflowMapper workflowMapper;
    private final WorkflowNodeMapper nodeMapper;
    private final WorkflowEdgeMapper edgeMapper;
    private final WorkflowInstanceMapper instanceMapper;
    private final NodeExecutionMapper nodeExecutionMapper;
    private final WorkflowEngine workflowEngine;

    @Override
    @Transactional
    public Long create(WorkflowCreateReq req) {
        WorkflowEntity entity = new WorkflowEntity();
        entity.setName(req.getName());
        entity.setDescription(req.getDescription());
        entity.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        workflowMapper.insert(entity);

        List<Long> nodeIds = saveNodes(entity.getId(), req.getNodes());
        saveEdges(entity.getId(), req.getEdges(), nodeIds);

        log.info("Workflow created: id={}, name={}", entity.getId(), entity.getName());
        return entity.getId();
    }

    @Override
    @Transactional
    public void update(Long id, WorkflowUpdateReq req) {
        WorkflowEntity entity = workflowMapper.selectById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw BizException.notFound("工作流不存在");
        }

        if (req.getName() != null) entity.setName(req.getName());
        if (req.getDescription() != null) entity.setDescription(req.getDescription());
        if (req.getStatus() != null) entity.setStatus(req.getStatus());
        workflowMapper.updateById(entity);

        if (req.getNodes() != null) {
            List<WorkflowNodeEntity> existingNodes = nodeMapper.selectList(
                    new LambdaQueryWrapper<WorkflowNodeEntity>()
                            .eq(WorkflowNodeEntity::getWorkflowId, id)
                            .eq(WorkflowNodeEntity::getDeleted, 0)
                            .orderByAsc(WorkflowNodeEntity::getId));
            List<Long> nodeIds = syncNodes(id, existingNodes, req.getNodes());
            rebuildEdges(id, nodeIds, req.getEdges());
        }

        log.info("Workflow updated: id={}, name={}", id, entity.getName());
    }

    private List<Long> syncNodes(Long workflowId, List<WorkflowNodeEntity> existingNodes,
                                  List<WorkflowDto.NodeItem> nodeItems) {
        List<Long> nodeIds = new ArrayList<>();
        for (int i = 0; i < nodeItems.size(); i++) {
            WorkflowDto.NodeItem item = nodeItems.get(i);
            if (i < existingNodes.size()) {
                WorkflowNodeEntity node = existingNodes.get(i);
                node.setName(item.getName());
                node.setType(item.getType());
                node.setConfigJson(item.getConfigJson());
                node.setPositionX(item.getPositionX() != null ? item.getPositionX() : 0);
                node.setPositionY(item.getPositionY() != null ? item.getPositionY() : 0);
                nodeMapper.updateById(node);
                nodeIds.add(node.getId());
            } else {
                nodeIds.add(saveOneNode(workflowId, item));
            }
        }
        for (int i = nodeItems.size(); i < existingNodes.size(); i++) {
            nodeMapper.deleteById(existingNodes.get(i).getId());
        }
        return nodeIds;
    }

    private void rebuildEdges(Long workflowId, List<Long> nodeIds,
                               List<WorkflowDto.EdgeItem> edgeItems) {
        edgeMapper.delete(new LambdaQueryWrapper<WorkflowEdgeEntity>()
                .eq(WorkflowEdgeEntity::getWorkflowId, workflowId));
        if (edgeItems != null) {
            saveEdges(workflowId, edgeItems, nodeIds);
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        WorkflowEntity entity = workflowMapper.selectById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw BizException.notFound("工作流不存在");
        }
        workflowMapper.deleteById(id);
        log.info("Workflow deleted: id={}", id);
    }

    @Override
    public PageResult<WorkflowResp> list(WorkflowListParams params) {
        Page<WorkflowEntity> page = PageHelper.toPage(params.getPage(), params.getPageSize());
        var wrapper = new LambdaQueryWrapper<WorkflowEntity>()
                .eq(WorkflowEntity::getDeleted, 0)
                .orderByDesc(WorkflowEntity::getCreatedAt);
        if (params.getName() != null && !params.getName().isBlank()) {
            wrapper.like(WorkflowEntity::getName, params.getName());
        }
        if (params.getStatus() != null) {
            wrapper.eq(WorkflowEntity::getStatus, params.getStatus());
        }

        IPage<WorkflowEntity> pageResult = workflowMapper.selectPage(page, wrapper);
        List<WorkflowResp> list = pageResult.getRecords().stream()
                .map(this::toResp)
                .toList();

        return PageResult.ok(list, pageResult.getTotal(), pageResult.getCurrent(), pageResult.getSize());
    }

    @Override
    public WorkflowResp getDetail(Long id) {
        WorkflowEntity entity = workflowMapper.selectById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw BizException.notFound("工作流不存在");
        }
        return toResp(entity);
    }

    @Override
    public WorkflowInstanceResp run(Long id, WorkflowRunReq req) {
        Map<String, Object> input = req.getInput() != null ? req.getInput() : Map.of();
        String triggerType = req.getSessionId() != null ? "agent" : "api";
        ExecutionState execState = ExecutionState.builder()
                .sessionId(req.getSessionId())
                .triggerType(triggerType)
                .modelConfigId(req.getModelConfigId())
                .tools(req.getTools())
                .build();
        WorkflowInstanceEntity instance = workflowEngine.execute(id, input, execState);
        return toInstanceResp(instance);
    }

    @Override
    public PageResult<WorkflowInstanceResp> listInstances(Long workflowId, Integer page, Integer pageSize) {
        Page<WorkflowInstanceEntity> pageParam = PageHelper.toPage(page, pageSize);
        var wrapper = new LambdaQueryWrapper<WorkflowInstanceEntity>()
                .eq(workflowId != null, WorkflowInstanceEntity::getWorkflowId, workflowId)
                .orderByDesc(WorkflowInstanceEntity::getCreatedAt);
        IPage<WorkflowInstanceEntity> pageResult = instanceMapper.selectPage(pageParam, wrapper);
        List<WorkflowInstanceResp> list = pageResult.getRecords().stream()
                .map(this::toInstanceResp)
                .toList();
        return PageResult.ok(list, pageResult.getTotal(), pageResult.getCurrent(), pageResult.getSize());
    }

    @Override
    public WorkflowInstanceResp getInstanceDetail(Long instanceId) {
        WorkflowInstanceEntity instance = instanceMapper.selectById(instanceId);
        if (instance == null) {
            throw BizException.notFound("执行实例不存在");
        }
        WorkflowInstanceResp resp = toInstanceResp(instance);
        List<NodeExecutionEntity> executions = nodeExecutionMapper.selectList(
                new LambdaQueryWrapper<NodeExecutionEntity>()
                        .eq(NodeExecutionEntity::getInstanceId, instanceId)
                        .orderByAsc(NodeExecutionEntity::getCreatedAt));
        resp.setNodeExecutions(executions.stream().map(e -> {
            NodeExecutionResp ner = new NodeExecutionResp();
            BeanUtils.copyProperties(e, ner);
            return ner;
        }).toList());
        return resp;
    }

    private Long saveOneNode(Long workflowId, WorkflowDto.NodeItem item) {
        WorkflowNodeEntity node = new WorkflowNodeEntity();
        node.setWorkflowId(workflowId);
        node.setName(item.getName());
        node.setType(item.getType());
        node.setConfigJson(item.getConfigJson());
        node.setPositionX(item.getPositionX() != null ? item.getPositionX() : 0);
        node.setPositionY(item.getPositionY() != null ? item.getPositionY() : 0);
        nodeMapper.insert(node);
        return node.getId();
    }

    private List<Long> saveNodes(Long workflowId, List<WorkflowDto.NodeItem> nodeItems) {
        List<Long> nodeIds = new ArrayList<>();
        for (WorkflowDto.NodeItem item : nodeItems) {
            nodeIds.add(saveOneNode(workflowId, item));
        }
        return nodeIds;
    }

    private void saveEdges(Long workflowId, List<WorkflowDto.EdgeItem> edgeItems, List<Long> nodeIds) {
        for (WorkflowDto.EdgeItem item : edgeItems) {
            WorkflowEdgeEntity edge = new WorkflowEdgeEntity();
            edge.setWorkflowId(workflowId);
            edge.setSourceNodeId(nodeIds.get(item.getSourceNodeIndex()));
            edge.setTargetNodeId(nodeIds.get(item.getTargetNodeIndex()));
            edge.setEdgeType(item.getEdgeType() != null ? item.getEdgeType() : "normal");
            edge.setConditionExpr(item.getConditionExpr());
            edge.setSortOrder(item.getSortOrder() != null ? item.getSortOrder() : 0);
            edgeMapper.insert(edge);
        }
    }

    private WorkflowResp toResp(WorkflowEntity entity) {
        WorkflowResp resp = new WorkflowResp();
        BeanUtils.copyProperties(entity, resp);

        List<WorkflowNodeEntity> nodes = nodeMapper.selectList(
                new LambdaQueryWrapper<WorkflowNodeEntity>()
                        .eq(WorkflowNodeEntity::getWorkflowId, entity.getId())
                        .eq(WorkflowNodeEntity::getDeleted, 0));
        List<WorkflowDto.NodeItem> nodeItems = new ArrayList<>();
        for (WorkflowNodeEntity n : nodes) {
            WorkflowDto.NodeItem item = new WorkflowDto.NodeItem();
            item.setId(n.getId());
            item.setName(n.getName());
            item.setType(n.getType());
            item.setConfigJson(n.getConfigJson());
            item.setPositionX(n.getPositionX());
            item.setPositionY(n.getPositionY());
            nodeItems.add(item);
        }
        resp.setNodes(nodeItems);

        List<WorkflowEdgeEntity> edges = edgeMapper.selectList(
                new LambdaQueryWrapper<WorkflowEdgeEntity>()
                        .eq(WorkflowEdgeEntity::getWorkflowId, entity.getId())
                        .eq(WorkflowEdgeEntity::getDeleted, 0));
        Map<Long, Integer> idToIndex = new HashMap<>();
        for (int i = 0; i < nodes.size(); i++) {
            idToIndex.put(nodes.get(i).getId(), i);
        }
        List<WorkflowDto.EdgeItem> edgeItems = new ArrayList<>();
        for (WorkflowEdgeEntity e : edges) {
            WorkflowDto.EdgeItem item = new WorkflowDto.EdgeItem();
            item.setSourceNodeIndex(idToIndex.get(e.getSourceNodeId()));
            item.setTargetNodeIndex(idToIndex.get(e.getTargetNodeId()));
            item.setEdgeType(e.getEdgeType());
            item.setConditionExpr(e.getConditionExpr());
            item.setSortOrder(e.getSortOrder());
            edgeItems.add(item);
        }
        resp.setEdges(edgeItems);

        return resp;
    }

    private WorkflowInstanceResp toInstanceResp(WorkflowInstanceEntity entity) {
        WorkflowInstanceResp resp = new WorkflowInstanceResp();
        BeanUtils.copyProperties(entity, resp);
        return resp;
    }
}
