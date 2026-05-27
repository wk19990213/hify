package com.hify.knowledge.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hify.provider.entity.ProviderEntity;
import com.hify.provider.mapper.ProviderMapper;
import com.hify.common.util.UrlSecurityValidator;
import com.hify.provider.util.AuthConfigHelper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import java.util.Map;

/**
 * Embedding 服务 —— 调用外部 Embedding API（OpenAI 兼容格式）。
 * 优先使用第一个启用的 Provider 的 baseUrl 和 API Key，
 * 如果没有 Provider 则回退到 Ollama 本地 (localhost:11434)。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final ObjectMapper objectMapper;
    private final ProviderMapper providerMapper;

    /** 硅基流动直达（避免遍历死 Provider 超时） */
    @Value("${embedding.url:https://api.siliconflow.cn/v1/embeddings}")
    private String embeddingUrl;

    @Value("${embedding.key:}")
    private String embeddingKey;

    @PostConstruct
    void checkConfig() {
        if ((embeddingKey == null || embeddingKey.isEmpty()) && embeddingUrl != null
                && !embeddingUrl.contains("localhost") && !embeddingUrl.contains("127.0.0.1")) {
            log.warn("EMBEDDING_KEY 未配置但 embedding.url 指向远程地址({})，将回退到 Ollama 本地服务", embeddingUrl);
        }
    }

    /** 将文本转为向量 */
    public float[] embed(String text, String model) {
        // 直接调硅基流动（如果配置了密钥）
        if (embeddingKey != null && !embeddingKey.isEmpty()) {
            float[] vec = callApi(embeddingUrl, embeddingKey, model != null ? model : "BAAI/bge-m3", text);
            if (vec.length > 0) return vec;
        }
        // 回退 Ollama
        return embedViaOllama(text, model);
    }

    private float[] callApi(String url, String apiKey, String model, String text) {
        try {
            UrlSecurityValidator.validateUrl(url, "embeddingUrl");
            String body = objectMapper.writeValueAsString(Map.of("model", model, "input", text));
            HttpRequest.Builder req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(body));
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            HttpResponse<String> resp = client.send(req.build(), HttpResponse.BodyHandlers.ofString());
            int statusCode = resp.statusCode();
            if (statusCode != 200) {
                log.warn("Embedding API returned HTTP {}: {}", statusCode,
                        resp.body().substring(0, Math.min(100, resp.body().length())));
                return new float[0];
            }
            JsonNode root = objectMapper.readTree(resp.body());
            JsonNode data = root.get("data");
            if (data != null && data.size() > 0) {
                JsonNode embedding = data.get(0).get("embedding");
                float[] vec = new float[embedding.size()];
                for (int i = 0; i < embedding.size(); i++) vec[i] = (float) embedding.get(i).asDouble();
                return vec;
            }
            log.warn("Embedding API returned unexpected response: {}", resp.body().substring(0, Math.min(200, resp.body().length())));
        } catch (Exception e) {
            log.warn("Embedding API call failed: {}", e.getMessage());
        }
        return new float[0];
    }

    /** OpenAI 兼容格式: POST /v1/embeddings */
    private float[] embedViaOpenAiApi(String text, String model, ProviderEntity provider) {
        try {
            String baseUrl = provider.getBaseUrl();
            if (baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            String url = baseUrl + (baseUrl.endsWith("/v1") ? "/embeddings" : "/v1/embeddings");

            UrlSecurityValidator.validateUrl(url, "embeddingUrl");

            String apiKey = extractApiKey(provider);
            String body = objectMapper.writeValueAsString(Map.of(
                    "model", model != null ? model : "BAAI/bge-m3",
                    "input", text
            ));

            HttpRequest.Builder req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(body));
            if (apiKey != null) req.header("Authorization", "Bearer " + apiKey);

            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
            HttpResponse<String> resp = client.send(req.build(), HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(resp.body());
            JsonNode data = root.get("data");
            if (data != null && data.size() > 0) {
                JsonNode embedding = data.get(0).get("embedding");
                float[] vec = new float[embedding.size()];
                for (int i = 0; i < embedding.size(); i++) vec[i] = (float) embedding.get(i).asDouble();
                return vec;
            }
        } catch (Exception e) {
            log.warn("OpenAI embeddings failed for {}: {}", provider.getName(), e.getMessage());
        }
        return new float[0];
    }

    /** Ollama 本地: POST /api/embeddings */
    private float[] embedViaOllama(String text, String model) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "model", model != null ? model : "nomic-embed-text",
                    "prompt", text));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:11434/api/embeddings"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(resp.body());
            JsonNode arr = root.get("embedding");
            float[] vec = new float[arr.size()];
            for (int i = 0; i < arr.size(); i++) vec[i] = (float) arr.get(i).asDouble();
            return vec;
        } catch (Exception e) {
            log.error("Ollama embedding failed: {}", e.getMessage());
            return new float[0];
        }
    }

    private String extractApiKey(ProviderEntity provider) {
        return AuthConfigHelper.extractApiKey(provider.getAuthConfig());
    }

    public String vecToString(float[] vec) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vec.length; i++) { if (i > 0) sb.append(","); sb.append(vec[i]); }
        sb.append("]"); return sb.toString();
    }

    public float[] stringToVec(String json) {
        try {
            JsonNode arr = objectMapper.readTree(json);
            float[] vec = new float[arr.size()];
            for (int i = 0; i < arr.size(); i++) vec[i] = (float) arr.get(i).asDouble();
            return vec;
        } catch (Exception e) { return new float[0]; }
    }

    public static double cosineSimilarity(float[] a, float[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) { dot += a[i] * b[i]; normA += a[i] * a[i]; normB += b[i] * b[i]; }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB) + 1e-8);
    }
}
