package com.hify.provider.service;

import com.hify.common.util.JsonUtils;
import com.hify.mcp.mcp.McpClientManager;
import com.hify.mcp.mcp.ToolDef;
import com.hify.mcp.mcp.ToolResult;
import com.hify.provider.adapter.ProviderAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 工具调用处理器 — LLM tool_calls → 消息构建 + MCP 工具执行。
 * 消除 ChatServiceImpl / LlmNodeExecutor 中的工具调用循环重复。
 * 放在 hify-provider 模块以避免 hify-mcp ↔ hify-provider 循环依赖。
 */
@Component
@RequiredArgsConstructor
public class ToolCallHandler {

    private final McpClientManager mcpClientManager;

    /** 执行工具调用并追加 assistant+tool 消息到消息列表 */
    public void executeToolCalls(ProviderAdapter adapter, String llmResponse,
                                  List<ProviderAdapter.ToolCall> toolCalls,
                                  List<ToolDef> tools,
                                  List<Map<String, Object>> messages) {
        String assistantContent = adapter.extractContent(llmResponse);
        List<Map<String, Object>> tcMaps = new ArrayList<>();
        for (ProviderAdapter.ToolCall tc : toolCalls) {
            Map<String, Object> func = new LinkedHashMap<>();
            func.put("name", tc.getName());
            func.put("arguments", JsonUtils.toJson(tc.getArguments()));
            tcMaps.add(Map.of("id", tc.getId() != null ? tc.getId() : "",
                    "type", "function", "function", func));
        }
        Map<String, Object> asstMsg = new LinkedHashMap<>();
        asstMsg.put("role", "assistant");
        asstMsg.put("content", assistantContent != null ? assistantContent : "");
        asstMsg.put("tool_calls", tcMaps);
        messages.add(asstMsg);

        for (ProviderAdapter.ToolCall tc : toolCalls) {
            ToolDef def = findToolDef(tools, tc.getName());
            Long serverId = def != null ? def.getServerId() : null;
            if (serverId == null) continue;
            ToolResult tr = mcpClientManager.callTool(serverId, tc.getName(), tc.getArguments());
            Map<String, Object> toolMsg = new LinkedHashMap<>();
            toolMsg.put("role", "tool");
            toolMsg.put("tool_call_id", tc.getId() != null ? tc.getId() : "");
            toolMsg.put("content", tr.isSuccess() ? tr.getContent() : "Error: " + tr.getError());
            messages.add(toolMsg);
        }
    }

    public ToolDef findToolDef(List<ToolDef> tools, String name) {
        if (tools == null) return null;
        return tools.stream().filter(t -> t.getName().equals(name)).findFirst().orElse(null);
    }
}
