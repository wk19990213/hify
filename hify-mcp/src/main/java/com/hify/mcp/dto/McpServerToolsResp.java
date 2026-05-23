package com.hify.mcp.dto;

import com.hify.mcp.mcp.ToolDef;
import lombok.Data;

import java.util.List;

/** MCP 服务器工具聚合响应 */
@Data
public class McpServerToolsResp {
    private Long serverId;
    private String serverName;
    private List<ToolDef> tools;
    private String errorMsg;
}
