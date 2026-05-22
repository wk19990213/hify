package com.hify.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("node_execution")
public class NodeExecutionEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long instanceId;
    private Long nodeId;
    private String status;
    private String inputJson;
    private String outputJson;
    private String errorMsg;
    private Integer retryCount;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
}
