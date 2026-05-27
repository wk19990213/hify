package com.hify.workflow.engine;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hify.common.exception.BizException;
import com.hify.mcp.mcp.ToolDef;
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

    public WorkflowInstanceEntity execute(Long workflowId, Map<String, Object> input, Long sessionId, String triggerType, Long modelConfigId, List<ToolDef> tools) {
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

        DagGraph dag = buildDagGraph(nodes, edges);
        Long startNodeId = findStartNode(dag.inDegree);
        WorkflowInstanceEntity instance = createInstance(workflowId, sessionId, triggerType, input);

        // 5. 执行 DAG
        Map<String, Object> variables = new HashMap<>();
        variables.put("input", input);

        try {
            Object lastOutput = executeNode(startNodeId, dag.nodeMap, dag.outEdges, variables, instance.getId(), modelConfigId, tools);
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
                               Map<String, Object> variables, Long instanceId, Long modelConfigId,
                               List<ToolDef> tools) {
        WorkflowNodeEntity node = nodeMap.get(nodeId);
        if (node == null) return null;

        NodeExecutionEntity exec = prepareNodeExecution(nodeId, instanceId, variables);

        // 获取执行器
        NodeExecutor executor = findExecutor(node.getType());
        if (executor == null) {
            exec.setStatus("failed");
            exec.setErrorMsg("未知节点类型: " + node.getType());
            exec.setFinishedAt(LocalDateTime.now());
            nodeExecutionMapper.updateById(exec);
            throw new RuntimeException("未知节点类型: " + node.getType());
        }

        NodeExecResult result = executeWithRetry(executor, node, nodeId, variables,
                modelConfigId, tools, exec);

        if (!result.isSuccess()) {
            return handleErrorRouting(node, nodeId, nodeMap, outEdges, variables, instanceId, modelConfigId, tools);
        }

        String varKey = generateVarKey(node.getName(), variables);
        variables.put(varKey, result.getOutput());
        variables.put(String.valueOf(nodeId), result.getOutput());

        return handleNextRouting(node, nodeId, result, nodeMap, outEdges, variables, instanceId, modelConfigId, tools);
    }

    /** 带重试执行节点并更新执行记录 */
    private NodeExecResult executeWithRetry(NodeExecutor executor, WorkflowNodeEntity node,
            Long nodeId, Map<String, Object> variables, Long modelConfigId,
            List<ToolDef> tools, NodeExecutionEntity exec) {
        NodeExecContext ctx = new NodeExecContext();
        ctx.setNode(node);
        ctx.setVariables(variables);
        ctx.setModelConfigId(modelConfigId);
        ctx.setTools(tools);

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

        exec.setStatus(result.isSuccess() ? "success" : "failed");
        exec.setErrorMsg(result.getErrorMsg());
        exec.setFinishedAt(LocalDateTime.now());
        try {
            exec.setOutputJson(objectMapper.writeValueAsString(result.getOutput()));
        } catch (Exception ignored) {}
        nodeExecutionMapper.updateById(exec);
        return result;
    }

    private NodeExecutionEntity prepareNodeExecution(Long nodeId, Long instanceId,
                                                       Map<String, Object> variables) {
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
        return exec;
    }

    private NodeExecutor findExecutor(String type) {
        for (NodeExecutor executor : executors) {
            if (executor.getType().equals(type)) {
                return executor;
            }
        }
        return null;
    }

    private String generateVarKey(String name, Map<String, Object> variables) {
        if (!variables.containsKey(name)) return name;
        int suffix = 2;
        while (variables.containsKey(name + "_" + suffix)) suffix++;
        return name + "_" + suffix;
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

    // -- DAG 构建 -----------------------------------------------

    private DagGraph buildDagGraph(List<WorkflowNodeEntity> nodes, List<WorkflowEdgeEntity> edges) {
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
        return new DagGraph(nodeMap, outEdges, inDegree);
    }

    private Long findStartNode(Map<Long, Integer> inDegree) {
        for (Map.Entry<Long, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) return entry.getKey();
        }
        throw BizException.paramError("工作流存在环路，无法找到起始节点");
    }

    private WorkflowInstanceEntity createInstance(Long workflowId, Long sessionId,
                                                   String triggerType, Map<String, Object> input) {
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
        return instance;
    }

    private Object handleErrorRouting(WorkflowNodeEntity node, Long nodeId,
            Map<Long, WorkflowNodeEntity> nodeMap, Map<Long, List<WorkflowEdgeEntity>> outEdges,
            Map<String, Object> variables, Long instanceId, Long modelConfigId, List<ToolDef> tools) {
        for (WorkflowEdgeEntity edge : outEdges.get(nodeId)) {
            if ("error".equals(edge.getEdgeType())) {
                return executeNode(edge.getTargetNodeId(), nodeMap, outEdges, variables, instanceId, modelConfigId, tools);
            }
        }
        throw new RuntimeException("节点 " + node.getName() + " 执行失败: ...");
    }

    private Object handleNextRouting(WorkflowNodeEntity node, Long nodeId, NodeExecResult result,
            Map<Long, WorkflowNodeEntity> nodeMap, Map<Long, List<WorkflowEdgeEntity>> outEdges,
            Map<String, Object> variables, Long instanceId, Long modelConfigId, List<ToolDef> tools) {
        List<WorkflowEdgeEntity> edges = outEdges.get(nodeId);
        if (edges.isEmpty()) return result.getOutput();

        if ("condition".equals(node.getType()) && result.getOutput() instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> outputMap = (Map<String, Object>) result.getOutput();
            boolean cond = outputMap.get("result") instanceof Boolean && (Boolean) outputMap.get("result");
            String target = cond ? "true" : "false";
            for (WorkflowEdgeEntity edge : edges) {
                if (target.equals(edge.getEdgeType())) {
                    return executeNode(edge.getTargetNodeId(), nodeMap, outEdges, variables, instanceId, modelConfigId, tools);
                }
            }
            return result.getOutput();
        }

        for (WorkflowEdgeEntity edge : edges) {
            if ("normal".equals(edge.getEdgeType())) {
                return executeNode(edge.getTargetNodeId(), nodeMap, outEdges, variables, instanceId, modelConfigId, tools);
            }
        }
        return result.getOutput();
    }

    record DagGraph(Map<Long, WorkflowNodeEntity> nodeMap,
                    Map<Long, List<WorkflowEdgeEntity>> outEdges,
                    Map<Long, Integer> inDegree) {}
}
