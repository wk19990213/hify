package com.hify.mcp.dto;

import lombok.Data;

@Data
public class McpServerUpdateReq {
    private String name;
    private String url;
    private String authConfig;
    private String transportType;
    private Integer status;
}
