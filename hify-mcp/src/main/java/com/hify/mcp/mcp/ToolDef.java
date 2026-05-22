package com.hify.mcp.mcp;

import lombok.Data;
import java.util.Map;

@Data
public class ToolDef {
    private String name;
    private String description;
    private Map<String, Object> inputSchema;
    private Long serverId;
}
