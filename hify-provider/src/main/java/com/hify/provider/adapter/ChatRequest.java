package com.hify.provider.adapter;

import com.hify.mcp.mcp.ToolDef;
import java.util.List;
import java.util.Map;

/** 统一聊天请求，适配器内部使用 */
public record ChatRequest(
        String model,
        List<Map<String, Object>> messages,
        double temperature,
        boolean stream,
        List<ToolDef> tools
) {
    public ChatRequest(String model, List<Map<String, Object>> messages,
                       double temperature, boolean stream) {
        this(model, messages, temperature, stream, null);
    }
}
