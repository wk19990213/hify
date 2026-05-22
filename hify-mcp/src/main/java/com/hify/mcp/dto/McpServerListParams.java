package com.hify.mcp.dto;

import lombok.Data;

/** MCP 服务器列表查询参数 */
@Data
public class McpServerListParams {
    private Integer page = 1;
    private Integer pageSize = 20;
    private String name;
    private Integer status;
}
