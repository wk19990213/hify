package com.hify.workflow.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class WorkflowResp {
    private Long id;
    private String name;
    private String description;
    private Integer status;
    private List<WorkflowDto.NodeItem> nodes;
    private List<WorkflowDto.EdgeItem> edges;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
