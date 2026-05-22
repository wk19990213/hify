package com.hify.workflow.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class WorkflowInstanceResp {
    private Long id;
    private Long workflowId;
    private String workflowName;
    private Long sessionId;
    private String triggerType;
    private String status;
    private String inputJson;
    private String outputJson;
    private String errorMsg;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
    private List<NodeExecutionResp> nodeExecutions;
}
