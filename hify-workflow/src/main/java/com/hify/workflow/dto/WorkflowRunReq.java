package com.hify.workflow.dto;

import lombok.Data;
import java.util.Map;

@Data
public class WorkflowRunReq {
    private Map<String, Object> input;
    private Long sessionId;
    /** 模型配置 ID，由调用方（如 Agent）传入，LLM 节点将使用此模型 */
    private Long modelConfigId;
}
