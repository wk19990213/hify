package com.hify.provider.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hify.common.http.LlmHttpClient;

/**
 * OpenAI Compatible 适配器 —— 与 OpenAI 共用完全相同的逻辑（Bearer Token + /v1/models）。
 * 直接继承 {@link OpenAiAdapter}，无需额外代码。
 */
public class OpenAiCompatibleAdapter extends OpenAiAdapter {

    public OpenAiCompatibleAdapter(LlmHttpClient llmHttpClient, ObjectMapper objectMapper) {
        super(llmHttpClient, objectMapper);
    }
}
