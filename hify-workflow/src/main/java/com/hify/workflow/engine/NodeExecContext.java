package com.hify.workflow.engine;

import com.hify.workflow.entity.WorkflowNodeEntity;
import lombok.Data;

import java.util.Map;

@Data
public class NodeExecContext {
    private WorkflowNodeEntity node;
    private Map<String, Object> variables;
}
