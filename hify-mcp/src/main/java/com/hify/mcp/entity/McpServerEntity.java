package com.hify.mcp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hify.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * MCP 服务器实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mcp_server")
public class McpServerEntity extends BaseEntity {

    private String name;
    private String command;
    private String argsJson;
    private String envVarsJson;
    private String url;
    private String transportType;
    private Integer status;
}
