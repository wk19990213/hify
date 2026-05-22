package com.hify.workflow.engine;

import com.hify.mcp.mcp.ToolDef;
import com.hify.workflow.entity.WorkflowNodeEntity;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class NodeExecContext {
    private WorkflowNodeEntity node;
    private Map<String, Object> variables;
    /** 模型配置 ID，由调用方传入，LLM 节点优先使用此值 */
    private Long modelConfigId;
    /** 可用的 MCP 工具列表，由调用方传入，LLM 节点使用 */
    private List<ToolDef> tools;
}
