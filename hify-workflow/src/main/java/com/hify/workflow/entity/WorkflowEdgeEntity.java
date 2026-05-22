package com.hify.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hify.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("workflow_edge")
public class WorkflowEdgeEntity extends BaseEntity {
    private Long workflowId;
    private Long sourceNodeId;
    private Long targetNodeId;
    private String edgeType;
    private String conditionExpr;
    private Integer sortOrder;
}
