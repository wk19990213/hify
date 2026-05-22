package com.hify.workflow.dto;

import lombok.Data;

@Data
public class WorkflowListParams {
    private Integer page = 1;
    private Integer pageSize = 20;
    private String name;
    private Integer status;
}
