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
        long start = System.currentTimeMillis();
        log.info("MCP tool call start: serverId={}, tool={}, args={}", serverId, name, arguments);
        try {
            McpClient client = getOrCreate(serverId);
            ToolResult result = client.callTool(name, arguments);
            long cost = System.currentTimeMillis() - start;
            log.info("MCP tool call done: serverId={}, tool={}, cost={}ms, success={}",
                    serverId, name, cost, result.isSuccess());
            return result;
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - start;
            log.error("MCP tool call failed: serverId={}, tool={}, cost={}ms", serverId, name, cost, e);
            ToolResult r = new ToolResult();
            r.setSuccess(false);
            r.setError(e.getMessage());
            return r;
        }
    }

    public boolean healthCheck(Long serverId) {
        try {
            McpClient client = getOrCreate(serverId);
            client.listTools();
            return true;
        } catch (Exception e) {
            log.warn("MCP health check failed for server {}", serverId, e.getMessage());
            return false;
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
        return new HttpJsonRpcTransport(server.getUrl(), server.getAuthConfig());
    }

    public void evict(Long serverId) {
        McpClient client = clients.remove(serverId);
        if (client != null) client.close();
    }
}
