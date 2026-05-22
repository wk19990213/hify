package com.hify.mcp.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hify.mcp.entity.McpServerEntity;
import com.hify.mcp.mapper.McpServerMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class McpClientManager {
    private final McpServerMapper serverMapper;
    private final ObjectMapper objectMapper;
    private final Map<Long, McpClient> clients = new ConcurrentHashMap<>();

    public List<ToolDef> listTools(Long serverId) {
        try {
            McpClient client = getOrCreate(serverId);
            List<ToolDef> tools = client.listTools();
            for (ToolDef t : tools) t.setServerId(serverId);
            return tools;
        } catch (Exception e) {
            log.error("Failed to list tools for MCP server {}", serverId, e);
            return List.of();
        }
    }

    public ToolResult callTool(Long serverId, String name,
                               Map<String, Object> arguments) {
        try {
            McpClient client = getOrCreate(serverId);
            return client.callTool(name, arguments);
        } catch (Exception e) {
            log.error("Failed to call tool {} on server {}", name, serverId, e);
            ToolResult r = new ToolResult();
            r.setSuccess(false);
            r.setError(e.getMessage());
            return r;
        }
    }

    private McpClient getOrCreate(Long serverId) throws Exception {
        return clients.computeIfAbsent(serverId, id -> {
            McpServerEntity server = serverMapper.selectById(id);
            if (server == null)
                throw new RuntimeException("MCP server not found: " + id);
            McpTransport transport = createTransport(server);
            return new McpClient(transport);
        });
    }

    private McpTransport createTransport(McpServerEntity server) {
        if ("sse".equals(server.getTransportType())) {
            return new SseTransport(server.getUrl());
        }
        List<String> args = parseArgs(server.getArgsJson());
        return new StdioTransport(server.getCommand(), args);
    }

    @SuppressWarnings("unchecked")
    private List<String> parseArgs(String argsJson) {
        if (argsJson == null || argsJson.isBlank()) return List.of();
        try {
            return objectMapper.readValue(argsJson, List.class);
        } catch (Exception e) {
            return List.of(argsJson);
        }
    }

    public void evict(Long serverId) {
        McpClient client = clients.remove(serverId);
        if (client != null) client.close();
    }
}
