package com.hify.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("workflow_edge")
public class WorkflowEdgeEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long workflowId;
    private Long sourceNodeId;
    private Long targetNodeId;
    private String edgeType;
    private String conditionExpr;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
