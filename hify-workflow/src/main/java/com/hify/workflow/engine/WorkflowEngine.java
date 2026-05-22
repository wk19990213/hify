package com.hify.workflow.engine;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hify.common.exception.BizException;
import com.hify.workflow.entity.*;
import com.hify.workflow.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowEngine {

    private final WorkflowMapper workflowMapper;
    private final WorkflowNodeMapper nodeMapper;
    private final WorkflowEdgeMapper edgeMapper;
    private final WorkflowInstanceMapper instanceMapper;
    private final NodeExecutionMapper nodeExecutionMapper;
    private final ObjectMapper objectMapper;
    private final List<NodeExecutor> executors;

    public WorkflowInstanceEntity execute(Long workflowId, Map<String, Object> input, Long sessionId, String triggerType) {
        // 1. 加载工作流定义
        WorkflowEntity workflow = workflowMapper.selectById(workflowId);
        if (workflow == null || workflow.getDeleted() == 1) {
            throw BizException.notFound("工作流不存在");
        }
        if (workflow.getStatus() == 0) {
            throw BizException.paramError("工作流已禁用");
        }

        List<WorkflowNodeEntity> nodes = nodeMapper.selectList(
                new LambdaQueryWrapper<WorkflowNodeEntity>()
                        .eq(WorkflowNodeEntity::getWorkflowId, workflowId)
                        .eq(WorkflowNodeEntity::getDeleted, 0));
        List<WorkflowEdgeEntity> edges = edgeMapper.selectList(
                new LambdaQueryWrapper<WorkflowEdgeEntity>()
                        .eq(WorkflowEdgeEntity::getWorkflowId, workflowId)
                        .eq(WorkflowEdgeEntity::getDeleted, 0));

        if (nodes.isEmpty()) {
            throw BizException.paramError("工作流没有节点");
        }

        // 2. 构建 DAG 邻接表 + 入度计算
        Map<Long, WorkflowNodeEntity> nodeMap = new HashMap<>();
        for (WorkflowNodeEntity node : nodes) {
            nodeMap.put(node.getId(), node);
        }

        Map<Long, Integer> inDegree = new HashMap<>();
        for (WorkflowNodeEntity node : nodes) {
            inDegree.put(node.getId(), 0);
        }
        Map<Long, List<WorkflowEdgeEntity>> outEdges = new HashMap<>();
        for (WorkflowNodeEntity node : nodes) {
            outEdges.put(node.getId(), new ArrayList<>());
        }
        for (WorkflowEdgeEntity edge : edges) {
            outEdges.get(edge.getSourceNodeId()).add(edge);
            inDegree.merge(edge.getTargetNodeId(), 1, Integer::sum);
        }

        // 3. 拓扑排序找起点（入度为 0 的节点）
        Long startNodeId = null;
        for (Map.Entry<Long, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                startNodeId = entry.getKey();
                break;
            }
        }
        if (startNodeId == null) {
            throw BizException.paramError("工作流存在环路，无法找到起始节点");
        }

        // 4. 创建执行实例
        WorkflowInstanceEntity instance = new WorkflowInstanceEntity();
        instance.setWorkflowId(workflowId);
        instance.setSessionId(sessionId);
        instance.setTriggerType(triggerType);
        instance.setStatus("running");
        try {
            instance.setInputJson(objectMapper.writeValueAsString(input));
        } catch (Exception ignored) {}
        instance.setStartedAt(LocalDateTime.now());
        instanceMapper.insert(instance);

        // 5. 执行 DAG
        Map<String, Object> variables = new HashMap<>();
        variables.put("input", input);

        try {
            Object lastOutput = executeNode(startNodeId, nodeMap, outEdges, variables, instance.getId());
            instance.setStatus("success");
            try {
                instance.setOutputJson(objectMapper.writeValueAsString(lastOutput));
            } catch (Exception ignored) {}
            instance.setFinishedAt(LocalDateTime.now());
            instanceMapper.updateById(instance);
        } catch (Exception e) {
            log.error("Workflow execution failed: instanceId={}", instance.getId(), e);
            instance.setStatus("failed");
            instance.setErrorMsg(e.getMessage());
            instance.setFinishedAt(LocalDateTime.now());
            instanceMapper.updateById(instance);
        }

        return instance;
    }

    private Object executeNode(Long nodeId, Map<Long, WorkflowNodeEntity> nodeMap,
                               Map<Long, List<WorkflowEdgeEntity>> outEdges,
                               Map<String, Object> variables, Long instanceId) {
        WorkflowNodeEntity node = nodeMap.get(nodeId);
        if (node == null) return null;

        // 创建节点执行记录
        NodeExecutionEntity exec = new NodeExecutionEntity();
        exec.setInstanceId(instanceId);
        exec.setNodeId(nodeId);
        exec.setStatus("running");
        exec.setRetryCount(0);
        exec.setStartedAt(LocalDateTime.now());
        try {
            exec.setInputJson(objectMapper.writeValueAsString(variables));
        } catch (Exception ignored) {}
        nodeExecutionMapper.insert(exec);

        // 获取执行器
        NodeExecutor executor = findExecutor(node.getType());
        if (executor == null) {
            exec.setStatus("failed");
            exec.setErrorMsg("未知节点类型: " + node.getType());
            exec.setFinishedAt(LocalDateTime.now());
            nodeExecutionMapper.updateById(exec);
            throw new RuntimeException("未知节点类型: " + node.getType());
        }

        // 执行节点（含重试）
        NodeExecContext ctx = new NodeExecContext();
        ctx.setNode(node);
        ctx.setVariables(variables);

        int maxRetries = getMaxRetries(node);
        NodeExecResult result = null;
        for (int retry = 0; retry <= maxRetries; retry++) {
            result = executor.execute(ctx);
            if (result.isSuccess()) break;
            if (retry < maxRetries) {
                log.warn("Node {} retry {}/{}: {}", nodeId, retry + 1, maxRetries, result.getErrorMsg());
                exec.setRetryCount(retry + 1);
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            }
        }

        // 更新执行记录
        exec.setStatus(result.isSuccess() ? "success" : "failed");
        exec.setErrorMsg(result.getErrorMsg());
        exec.setFinishedAt(LocalDateTime.now());
        try {
            exec.setOutputJson(objectMapper.writeValueAsString(result.getOutput()));
        } catch (Exception ignored) {}
        nodeExecutionMapper.updateById(exec);

        // 失败时检查 error 边
        if (!result.isSuccess()) {
            List<WorkflowEdgeEntity> edges = outEdges.get(nodeId);
            for (WorkflowEdgeEntity edge : edges) {
                if ("error".equals(edge.getEdgeType())) {
                    return executeNode(edge.getTargetNodeId(), nodeMap, outEdges, variables, instanceId);
                }
            }
            throw new RuntimeException("节点 " + node.getName() + " 执行失败: " + result.getErrorMsg());
        }

        // 存储输出到变量
        variables.put(String.valueOf(nodeId), result.getOutput());

        // 根据出边决定下一节点
        List<WorkflowEdgeEntity> edges = outEdges.get(nodeId);
        if (edges.isEmpty()) {
            return result.getOutput();
        }

        // 条件节点：评估结果决定走 true 还是 false 边
        if ("condition".equals(node.getType()) && result.getOutput() instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> outputMap = (Map<String, Object>) result.getOutput();
            boolean conditionResult = outputMap.get("result") instanceof Boolean
                    ? (Boolean) outputMap.get("result") : false;
            String targetEdgeType = conditionResult ? "true" : "false";
            for (WorkflowEdgeEntity edge : edges) {
                if (targetEdgeType.equals(edge.getEdgeType())) {
                    return executeNode(edge.getTargetNodeId(), nodeMap, outEdges, variables, instanceId);
                }
            }
            return result.getOutput();
        }

        // 普通节点：取第一条 normal 边
        for (WorkflowEdgeEntity edge : edges) {
            if ("normal".equals(edge.getEdgeType())) {
                return executeNode(edge.getTargetNodeId(), nodeMap, outEdges, variables, instanceId);
            }
        }

        return result.getOutput();
    }

    private NodeExecutor findExecutor(String type) {
        for (NodeExecutor executor : executors) {
            if (executor.getType().equals(type)) {
                return executor;
            }
        }
        return null;
    }

    private int getMaxRetries(WorkflowNodeEntity node) {
        try {
            Map<String, Object> config = objectMapper.readValue(node.getConfigJson(),
                    new TypeReference<Map<String, Object>>() {});
            if (config != null && config.containsKey("maxRetries")) {
                return ((Number) config.get("maxRetries")).intValue();
            }
        } catch (Exception ignored) {}
        return 0;
    }
}
