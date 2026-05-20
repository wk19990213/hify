package com.hify.provider.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hify.common.http.LlmHttpClient;
import com.hify.provider.constant.ProviderConstant;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provider 适配器工厂 —— 根据 provider.type 返回对应的 Adapter 实例。
 * 每个 type 的 Adapter 只创建一次（懒加载 + 缓存），线程安全。
 */
@Component
public class ProviderAdapterFactory {

    private final Map<String, ProviderAdapter> cache = new ConcurrentHashMap<>();
    private final LlmHttpClient llmHttpClient;
    private final ObjectMapper objectMapper;

    public ProviderAdapterFactory(LlmHttpClient llmHttpClient, ObjectMapper objectMapper) {
        this.llmHttpClient = llmHttpClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 根据 provider type 获取对应的适配器。
     * 不区分大小写，未知类型返回 null。
     */
    public ProviderAdapter getAdapter(String type) {
        if (type == null) {
            return null;
        }
        String key = type.toUpperCase();
        return cache.computeIfAbsent(key, this::createAdapter);
    }

    /** 工厂方法：按 type 实例化具体 Adapter（四种类型） */
    private ProviderAdapter createAdapter(String type) {
        return switch (type) {
            case ProviderConstant.TYPE_OPENAI -> new OpenAiAdapter(llmHttpClient, objectMapper);
            case ProviderConstant.TYPE_OPENAI_COMPATIBLE -> new OpenAiCompatibleAdapter(llmHttpClient, objectMapper);
            case ProviderConstant.TYPE_ANTHROPIC -> new AnthropicAdapter(llmHttpClient, objectMapper);
            case ProviderConstant.TYPE_OLLAMA -> new OllamaAdapter(llmHttpClient, objectMapper);
            default -> null;
        };
    }
}
