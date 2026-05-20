package com.hify.provider.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hify.common.http.LlmHttpClient;
import java.util.HashMap;
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
}
