package com.hify.mcp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** MCP 服务器创建请求 */
@Data
public class McpServerCreateReq {
    @NotBlank
    private String name;
    private String command;
    private String argsJson;
    private String envVarsJson;
    private String url;
    private String transportType = "stdio";
    private Integer status = 1;
}
