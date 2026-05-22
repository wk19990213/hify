package com.hify.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("workflow_instance")
public class WorkflowInstanceEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long workflowId;
    private Long sessionId;
    private String triggerType;
    private String status;
    private String inputJson;
    private String outputJson;
    private String errorMsg;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
}
