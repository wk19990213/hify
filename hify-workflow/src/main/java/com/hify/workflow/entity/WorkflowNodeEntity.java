package com.hify.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hify.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("workflow_node")
public class WorkflowNodeEntity extends BaseEntity {
    private Long workflowId;
    private String name;
    private String type;
    private String configJson;
    private Integer positionX;
    private Integer positionY;
}
