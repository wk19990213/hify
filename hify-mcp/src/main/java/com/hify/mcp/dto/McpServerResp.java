package com.hify.mcp.dto;

import lombok.Data;
import java.time.LocalDateTime;

/** MCP 服务器响应 */
@Data
public class McpServerResp {
    private Long id;
    private String name;
    private String command;
    private String argsJson;
    private String envVarsJson;
    private String url;
    private String transportType;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
