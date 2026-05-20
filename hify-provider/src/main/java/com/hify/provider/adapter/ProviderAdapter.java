package com.hify.provider.adapter;

import com.hify.common.http.StreamCallback;
import com.hify.provider.dto.ConnectionTestResult;
import com.hify.provider.entity.ProviderEntity;
import okhttp3.Call;

import java.util.Map;

/**
 * Provider 适配器接口 —— 策略模式，每种 provider type 对应一个实现。
 * 封装 URL、认证头、请求体、响应解析的差异。
 *
 * authConfig 由调用方解密后传入，适配器不关心加解密。
 */
public interface ProviderAdapter {

    /** 连通性测试 */
    ConnectionTestResult testConnection(ProviderEntity provider, Map<String, Object> authConfig);

    /**
     * 同步聊天 —— 构造请求体 → POST → 返回 LLM 原始 JSON 响应
     * @param baseUrl    Provider 的 base URL
     * @param authConfig 已解密的鉴权配置 Map
     * @param request    统一聊天请求
     * @return LLM 返回的原始 JSON 字符串，调用方自行解析
     */
    String chat(String baseUrl, Map<String, Object> authConfig, ChatRequest request);

    /**
     * 流式聊天 —— 构造请求体 → POST → 逐行回调
     * @return OkHttp Call，外部可调用 cancel() 终止请求
     */
    Call streamChat(String baseUrl, Map<String, Object> authConfig, ChatRequest request, StreamCallback callback);

    /** 从流式响应的一行 JSON 中提取文本增量（各适配器格式不同） */
    String extractDelta(String line);

    /** 从同步响应中提取 finish_reason */
    String extractFinishReason(String responseBody);

    /** 从同步响应中提取 usage.total_tokens */
    int extractTokenCount(String responseBody);

    /** 从同步响应中提取文本内容 */
    String extractContent(String responseBody);
}
