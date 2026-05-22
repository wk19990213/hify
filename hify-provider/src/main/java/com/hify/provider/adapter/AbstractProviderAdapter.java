package com.hify.provider.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hify.common.http.LlmApiException;
import com.hify.common.http.LlmHttpClient;
import com.hify.common.http.StreamCallback;
import com.hify.mcp.mcp.ToolDef;
import com.hify.provider.dto.ConnectionTestResult;
import com.hify.provider.entity.ProviderEntity;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 适配器抽象基类。
 * 子类实现 {@link #buildUrl}、{@link #buildHeaders}、{@link #buildChatUrl} 来区分类型。
 * OpenAI 兼容格式作为默认实现。
 */
@Slf4j
public abstract class AbstractProviderAdapter implements ProviderAdapter {

    static final int TEST_TIMEOUT_MS = 10000;

    final LlmHttpClient llmHttpClient;
    final ObjectMapper objectMapper;

    protected AbstractProviderAdapter(LlmHttpClient llmHttpClient, ObjectMapper objectMapper) {
        this.llmHttpClient = llmHttpClient;
        this.objectMapper = objectMapper;
    }

    /** 拼接模型列表 URL */
    abstract String buildUrl(String baseUrl);

    /** 拼接聊天 URL（OpenAI /v1/chat/completions，Anthropic /v1/messages，Ollama /api/chat） */
    abstract String buildChatUrl(String baseUrl);

    /** 构造认证 Header */
    abstract Map<String, String> buildHeaders(Map<String, Object> authConfig);

    /** Ollama 用 models 字段，其余用 data 字段 */
    String getModelsJsonKey() {
        return "data";
    }

    // ==================== testConnection ====================

    @Override
    public ConnectionTestResult testConnection(ProviderEntity provider, Map<String, Object> authConfig) {
        long start = System.currentTimeMillis();
        ConnectionTestResult result = new ConnectionTestResult();

        String url = buildUrl(provider.getBaseUrl());
        Map<String, String> headers = buildHeaders(authConfig);

        try {
            String responseBody = llmHttpClient.get(url, headers, TEST_TIMEOUT_MS);
            long latency = System.currentTimeMillis() - start;
            int modelCount = extractModelCount(responseBody);
            result.setSuccess(true);
            result.setLatencyMs(latency);
            result.setModelCount(modelCount);
        } catch (LlmApiException e) {
            result.setSuccess(false);
            result.setLatencyMs(System.currentTimeMillis() - start);
            result.setErrorMessage(e.getMessage());
        } catch (Exception e) {
            result.setSuccess(false);
            result.setLatencyMs(System.currentTimeMillis() - start);
            result.setErrorMessage(e.getMessage());
        }
        return result;
    }

    // ==================== chat / streamChat（OpenAI 兼容默认） ====================

    @Override
    public String chat(String baseUrl, Map<String, Object> authConfig, ChatRequest request) {
        Map<String, String> headers = buildHeaders(authConfig);
        headers.put("Content-Type", "application/json");
        String body = toJson(buildOpenAiBody(request));
        return llmHttpClient.post(buildChatUrl(baseUrl), headers, body);
    }

    @Override
    public Call streamChat(String baseUrl, Map<String, Object> authConfig, ChatRequest request, StreamCallback callback) {
        Map<String, String> headers = buildHeaders(authConfig);
        headers.put("Content-Type", "application/json");
        String body = toJson(buildOpenAiBody(request));
        return llmHttpClient.stream(buildChatUrl(baseUrl), headers, body, callback);
    }

    /** 构造 OpenAI 兼容请求体 */
    Map<String, Object> buildOpenAiBody(ChatRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", request.model());
        body.put("messages", request.messages());
        body.put("temperature", request.temperature());
        body.put("stream", request.stream());
        if (request.tools() != null && !request.tools().isEmpty()) {
            body.put("tools", formatTools(request.tools()));
        }
        return body;
    }

    // ==================== 响应解析（OpenAI 兼容默认） ====================

    @Override
    public String extractDelta(String line) {
        try {
            JsonNode root = objectMapper.readTree(line);
            JsonNode choices = root.get("choices");
            if (choices != null && choices.size() > 0) {
                JsonNode delta = choices.get(0).get("delta");
                if (delta != null && delta.has("content") && !delta.get("content").isNull()) {
                    return delta.get("content").asText();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    public String extractFinishReason(String responseBody) {
        return getJsonNode(responseBody, "choices", 0, "finish_reason");
    }

    @Override
    public int extractTokenCount(String responseBody) {
        String s = getJsonNode(responseBody, "usage", "total_tokens");
        if (s != null) {
            try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    @Override
    public String extractContent(String responseBody) {
        return getJsonNode(responseBody, "choices", 0, "message", "content");
    }

    @Override
    public List<String> listModelIds(String baseUrl, Map<String, Object> authConfig) {
        List<String> ids = new ArrayList<>();
        try {
            Map<String, String> headers = buildHeaders(authConfig);
            headers.put("Accept", "application/json");
            String body = llmHttpClient.get(buildUrl(baseUrl), headers, TEST_TIMEOUT_MS);
            JsonNode list = objectMapper.readTree(body).get(getModelsJsonKey());
            if (list != null) for (JsonNode m : list) ids.add(m.get("id").asText());
        } catch (Exception e) { log.warn("Failed to list models: {}", e.getMessage()); }
        return ids;
    }

    // ==================== 工具方法 ====================

    int extractModelCount(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode list = root.get(getModelsJsonKey());
            return list != null && list.isArray() ? list.size() : 0;
        } catch (Exception e) {
            log.warn("Failed to parse model count: {}", e.getMessage());
            return 0;
        }
    }

    String extractApiKey(Map<String, Object> authConfig) {
        if (authConfig == null) return null;
        Object apiKey = authConfig.get("apiKey");
        return apiKey != null ? apiKey.toString() : null;
    }

    static String normalizeBaseUrl(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    /** 将 ToolDef 列表转换为 OpenAI 工具格式，AnthropicAdapter 可覆盖 */
    protected List<Map<String, Object>> formatTools(List<ToolDef> tools) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (ToolDef t : tools) {
            Map<String, Object> func = new LinkedHashMap<>();
            func.put("name", t.getName());
            func.put("description", t.getDescription());
            if (t.getInputSchema() != null) {
                func.put("parameters", t.getInputSchema());
            }
            result.add(Map.of("type", "function", "function", func));
        }
        return result;
    }

    String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("JSON序列化失败", e);
        }
    }

    /** 从 JSON 路径取值：getJsonNode(body, "choices", 0, "message", "content") */
    private String getJsonNode(String body, Object... path) {
        try {
            JsonNode node = objectMapper.readTree(body);
            for (Object p : path) {
                if (node == null) return null;
                node = p instanceof Integer ? node.get((Integer) p) : node.get(p.toString());
            }
            return node != null && !node.isNull() ? node.asText() : null;
        } catch (Exception ignored) {
            return null;
        }
    }
}
