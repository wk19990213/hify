package com.hify.mcp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class McpServerCreateReq {
    @NotBlank
    private String name;
    @NotBlank
    private String url;
    private String authConfig;
    private String transportType = "http";
    private Integer status = 1;
}
