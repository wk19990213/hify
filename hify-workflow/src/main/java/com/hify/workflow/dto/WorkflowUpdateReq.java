package com.hify.workflow.dto;

import lombok.Data;
import java.util.List;

@Data
public class WorkflowUpdateReq {
    private String name;
    private String description;
    private Integer status;
    private List<WorkflowDto.NodeItem> nodes;
    private List<WorkflowDto.EdgeItem> edges;
}
