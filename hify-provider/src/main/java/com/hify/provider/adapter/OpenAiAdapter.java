package com.hify.provider.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hify.common.http.LlmHttpClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 适配器 —— /v1/models 测试，/v1/chat/completions 聊天，Bearer Token 认证。
 */
public class OpenAiAdapter extends AbstractProviderAdapter {

    public OpenAiAdapter(LlmHttpClient llmHttpClient, ObjectMapper objectMapper) {
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
        return n + (n.endsWith("/v1") ? "/chat/completions" : "/v1/chat/completions");
    }

    @Override
    Map<String, String> buildHeaders(Map<String, Object> authConfig) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        String apiKey = extractApiKey(authConfig);
        if (apiKey != null && !apiKey.isEmpty()) {
            headers.put("Authorization", "Bearer " + apiKey);
        }
        return headers;
    }

    @Override
    public List<ToolCall> extractToolCalls(String responseBody) {
        List<ToolCall> calls = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).get("message");
                JsonNode toolCalls = message.get("tool_calls");
                if (toolCalls != null && toolCalls.isArray()) {
                    for (JsonNode tc : toolCalls) {
                        ToolCall call = new ToolCall();
                        call.setId(tc.get("id").asText());
                        JsonNode func = tc.get("function");
                        call.setName(func.get("name").asText());
                        String argsJson = func.get("arguments").asText();
                        call.setArguments(objectMapper.readValue(argsJson, Map.class));
                        calls.add(call);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract OpenAI tool calls", e);
        }
        return calls;
    }
}
