package com.hify.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class WorkflowDto {

    @NotBlank(message = "名称不能为空")
    private String name;

    private String description;

    private Integer status;

    @NotNull(message = "节点列表不能为空")
    private List<NodeItem> nodes;

    @NotNull(message = "连线列表不能为空")
    private List<EdgeItem> edges;

    @Data
    public static class NodeItem {
        private Long id;
        private String name;
        @NotBlank(message = "节点类型不能为空")
        private String type;
        private String configJson;
        private Integer positionX;
        private Integer positionY;
    }

    @Data
    public static class EdgeItem {
        @NotNull(message = "源节点索引不能为空")
        private Integer sourceNodeIndex;
        @NotNull(message = "目标节点索引不能为空")
        private Integer targetNodeIndex;
        private String edgeType;
        private String conditionExpr;
        private Integer sortOrder;
    }
}
