package com.hify.mcp.dto;

import lombok.Data;

/** MCP 服务器更新请求 */
@Data
public class McpServerUpdateReq {
    private String name;
    private String command;
    private String argsJson;
    private String envVarsJson;
    private String url;
    private String transportType;
    private Integer status;
}
