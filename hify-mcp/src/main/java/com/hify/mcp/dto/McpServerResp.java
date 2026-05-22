package com.hify.mcp.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class McpServerResp {
    private Long id;
    private String name;
    private String url;
    private String authConfig;
    private String transportType;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
