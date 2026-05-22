package com.hify.mcp.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class McpClient implements AutoCloseable {
    private final McpTransport transport;
    private final ObjectMapper mapper = new ObjectMapper();

    public McpClient(McpTransport transport) {
        this.transport = transport;
    }

    public List<ToolDef> listTools() throws Exception {
        JsonNode result = transport.send("tools/list", null);
        List<ToolDef> tools = new ArrayList<>();
        JsonNode toolsNode = result.get("tools");
        if (toolsNode != null && toolsNode.isArray()) {
            for (JsonNode t : toolsNode) {
                ToolDef def = new ToolDef();
                def.setName(t.get("name").asText());
                def.setDescription(t.has("description")
                        ? t.get("description").asText() : "");
                if (t.has("inputSchema")) {
                    def.setInputSchema(mapper.convertValue(
                            t.get("inputSchema"), Map.class));
                }
                tools.add(def);
            }
        }
        return tools;
    }

    public ToolResult callTool(String name, Map<String, Object> arguments) {
        ToolResult result = new ToolResult();
        try {
            ObjectNode params = mapper.createObjectNode();
            params.put("name", name);
            params.set("arguments", mapper.valueToTree(
                    arguments != null ? arguments : Map.of()));
            JsonNode resp = transport.send("tools/call", params);
            JsonNode content = resp.get("content");
            if (content != null && content.isArray()) {
                StringBuilder sb = new StringBuilder();
                for (JsonNode c : content) {
                    String type = c.has("type") ? c.get("type").asText() : "text";
                    if ("text".equals(type)) {
                        sb.append(c.has("text") ? c.get("text").asText() : "");
                    }
                }
                result.setContent(sb.toString());
            }
            result.setSuccess(true);
        } catch (Exception e) {
            log.error("MCP tool call failed: {}", name, e);
            result.setSuccess(false);
            result.setError(e.getMessage());
        }
        return result;
    }

    @Override
    public void close() {
        try {
            transport.close();
        } catch (Exception ignored) {
        }
    }
}
