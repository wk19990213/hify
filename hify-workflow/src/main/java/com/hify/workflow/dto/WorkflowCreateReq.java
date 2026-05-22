package com.hify.workflow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class WorkflowCreateReq {
    @NotBlank(message = "名称不能为空")
    private String name;
    private String description;
    private Integer status;

    @NotNull(message = "节点列表不能为空")
    @Valid
    private List<WorkflowDto.NodeItem> nodes;

    @NotNull(message = "连线列表不能为空")
    @Valid
    private List<WorkflowDto.EdgeItem> edges;
}
