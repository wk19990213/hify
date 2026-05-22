package com.hify.workflow.dto;

import lombok.Data;
import java.util.Map;

@Data
public class WorkflowRunReq {
    private Map<String, Object> input;
    private Long sessionId;
}
