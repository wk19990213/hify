package com.hify.provider.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hify.common.http.LlmHttpClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ollama 适配器 —— /api/tags 测试，/api/chat 聊天，无需认证。
 */
public class OllamaAdapter extends AbstractProviderAdapter {

    public OllamaAdapter(LlmHttpClient llmHttpClient, ObjectMapper objectMapper) {
        super(llmHttpClient, objectMapper);
    }

    @Override
    String buildUrl(String baseUrl) {
        String n = normalizeBaseUrl(baseUrl);
        return n + (n.endsWith("/api") ? "/tags" : "/api/tags");
    }

    @Override
    String buildChatUrl(String baseUrl) {
        String n = normalizeBaseUrl(baseUrl);
        return n + (n.endsWith("/api") ? "/chat" : "/api/chat");
    }

    @Override
    Map<String, String> buildHeaders(Map<String, Object> authConfig) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        return headers;
    }

    @Override
    String getModelsJsonKey() {
        return "models";
    }

    /** Ollama 的 /api/chat 请求体比 OpenAI 简单，但可以兼容 */
    @Override
    Map<String, Object> buildOpenAiBody(ChatRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", request.model());
        // Ollama 不支持 system 消息独立，合并进第一条消息
        List<Map<String, String>> msgs = request.messages();
        body.put("messages", msgs);
        body.put("stream", request.stream());
        Map<String, Object> options = new HashMap<>();
        options.put("temperature", request.temperature());
        body.put("options", options);
        return body;
    }

    @Override
    public String extractDelta(String line) {
        // Ollama SSE: {"message": {"content": "..."}}
        try {
            JsonNode root = objectMapper.readTree(line);
            JsonNode msg = root.get("message");
            if (msg != null && msg.has("content") && !msg.get("content").isNull()) {
                return msg.get("content").asText();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    public String extractContent(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode msg = root.get("message");
            if (msg != null && msg.has("content")) {
                return msg.get("content").asText();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    public String extractFinishReason(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            return root.has("done_reason") ? root.get("done_reason").asText() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    public int extractTokenCount(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            if (root.has("eval_count")) {
                return root.get("eval_count").asInt();
            }
        } catch (Exception ignored) {
        }
        return 0;
    }
}
