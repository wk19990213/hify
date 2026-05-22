package com.hify.workflow.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NodeExecutionResp {
    private Long id;
    private Long instanceId;
    private Long nodeId;
    private String nodeName;
    private String nodeType;
    private String status;
    private String inputJson;
    private String outputJson;
    private String errorMsg;
    private Integer retryCount;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
