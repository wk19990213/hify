package com.hify.provider.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hify.common.http.LlmHttpClient;
import com.hify.common.http.StreamCallback;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Anthropic 适配器 —— 使用 Messages API（/v1/messages），格式与 OpenAI 完全不同。
 */
public class AnthropicAdapter extends AbstractProviderAdapter {

    public AnthropicAdapter(LlmHttpClient llmHttpClient, ObjectMapper objectMapper) {
        super(llmHttpClient, objectMapper);
    }

    @Override
    String buildUrl(String baseUrl) {
        String n = normalizeBaseUrl(baseUrl);
        return n + (n.endsWith("/v1") ? "/models" : "/v1/models");
    }

    @Override
    String buildChatUrl(String baseUrl) {
        String n = normalizeBaseUrl(baseUrl);
        return n + (n.endsWith("/v1") ? "/messages" : "/v1/messages");
    }

    @Override
    Map<String, String> buildHeaders(Map<String, Object> authConfig) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        String apiKey = extractApiKey(authConfig);
        if (apiKey != null && !apiKey.isEmpty()) {
            headers.put("x-api-key", apiKey);
        }
        headers.put("anthropic-version", "2023-06-01");
        return headers;
    }

    /** Anthropic 消息列表格式：去掉 system 消息，把它放到顶层 system 字段 */
    @Override
    Map<String, Object> buildOpenAiBody(ChatRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", request.model());
        body.put("max_tokens", 4096);
        body.put("stream", request.stream());

        // 提取 system 消息
        List<Map<String, String>> msgs = request.messages();
        for (Map<String, String> m : msgs) {
            if ("system".equals(m.get("role"))) {
                body.put("system", m.get("content"));
                break;
            }
        }
        // 非 system 消息
        List<Map<String, Object>> anthropicMsgs = new java.util.ArrayList<>();
        for (Map<String, String> m : msgs) {
            if (!"system".equals(m.get("role"))) {
                anthropicMsgs.add(Map.of("role", m.get("role"), "content", m.get("content")));
            }
        }
        body.put("messages", anthropicMsgs);
        return body;
    }

    @Override
    public String extractDelta(String line) {
        // Anthropic SSE: event: content_block_delta → data: {"delta": {"text": "..."}}
        try {
            JsonNode root = objectMapper.readTree(line);
            JsonNode delta = root.get("delta");
            if (delta != null && delta.has("text") && !delta.get("text").isNull()) {
                return delta.get("text").asText();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    public String extractContent(String responseBody) {
        // Anthropic: content[0].text
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode content = root.get("content");
            if (content != null && content.size() > 0) {
                JsonNode text = content.get(0).get("text");
                return text != null ? text.asText() : null;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    public String extractFinishReason(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode sr = root.get("stop_reason");
            return sr != null ? sr.asText() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    public int extractTokenCount(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode usage = root.get("usage");
            if (usage != null && usage.has("input_tokens") && usage.has("output_tokens")) {
                return usage.get("input_tokens").asInt() + usage.get("output_tokens").asInt();
            }
        } catch (Exception ignored) {
        }
        return 0;
    }
}
