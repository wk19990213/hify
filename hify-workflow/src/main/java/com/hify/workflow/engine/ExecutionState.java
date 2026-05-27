package com.hify.workflow.engine;

import com.hify.mcp.mcp.ToolDef;
import com.hify.workflow.entity.WorkflowEdgeEntity;
import com.hify.workflow.entity.WorkflowNodeEntity;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 工作流执行状态参数对象，封装 execute/executeNode/executeWithRetry 的公共参数。
 */
@Data
@Builder
public class ExecutionState {

    /** 节点 Map（nodeId -> WorkflowNodeEntity），由引擎内部填充 */
    private Map<Long, WorkflowNodeEntity> nodeMap;

    /** 出边 Map（nodeId -> 出边列表），由引擎内部填充 */
    private Map<Long, List<WorkflowEdgeEntity>> outEdges;

    /** 执行变量，由引擎内部初始化 */
    private Map<String, Object> variables;

    /** 工作流实例 ID，由引擎内部填充 */
    private Long instanceId;

    /** 模型配置 ID，调用方传入 */
    private Long modelConfigId;

    /** MCP 工具列表，调用方传入 */
    private List<ToolDef> tools;

    /** 会话 ID，调用方传入（createInstance 使用） */
    private Long sessionId;

    /** 触发类型，调用方传入（createInstance 使用） */
    private String triggerType;
}
