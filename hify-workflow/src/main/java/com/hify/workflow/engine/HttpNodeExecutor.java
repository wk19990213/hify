package com.hify.workflow.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class HttpNodeExecutor implements NodeExecutor {

    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    @Override
    public String getType() {
        return "http";
    }

    @Override
    public NodeExecResult execute(NodeExecContext ctx) {
        String configJson = ctx.getNode().getConfigJson();
        Map<String, Object> config;
        try {
            config = objectMapper.readValue(configJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return NodeExecResult.builder().success(false).errorMsg("HTTP 节点配置解析失败: " + e.getMessage()).build();
        }

        String url = (String) config.get("url");
        String method = config.get("method") != null ? (String) config.get("method") : "GET";
        String body = (String) config.get("body");
        @SuppressWarnings("unchecked")
        Map<String, String> headers = (Map<String, String>) config.get("headers");

        if (url == null) {
            return NodeExecResult.builder().success(false).errorMsg("HTTP 节点缺少 URL").build();
        }

        url = resolveVariables(url, ctx.getVariables());
        if (body != null) {
            body = resolveVariables(body, ctx.getVariables());
        }

        try {
            Request.Builder builder = new Request.Builder().url(url);
            if (headers != null) {
                headers.forEach(builder::addHeader);
            }

            RequestBody requestBody = null;
            if (body != null && !body.isEmpty() && ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method))) {
                requestBody = RequestBody.create(body, MediaType.parse("application/json"));
            }

            if ("GET".equalsIgnoreCase(method)) {
                builder.get();
            } else if ("POST".equalsIgnoreCase(method)) {
                builder.post(requestBody != null ? requestBody : RequestBody.create("", MediaType.parse("application/json")));
            } else if ("PUT".equalsIgnoreCase(method)) {
                builder.put(requestBody != null ? requestBody : RequestBody.create("", MediaType.parse("application/json")));
            } else if ("DELETE".equalsIgnoreCase(method)) {
                builder.delete();
            } else {
                builder.method(method, requestBody);
            }

            try (Response response = httpClient.newCall(builder.build()).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                return NodeExecResult.builder().success(response.isSuccessful())
                        .output(Map.of("status", response.code(), "body", responseBody))
                        .errorMsg(response.isSuccessful() ? null : "HTTP " + response.code())
                        .build();
            }
        } catch (IOException e) {
            log.error("HTTP node execution failed: nodeId={}", ctx.getNode().getId(), e);
            return NodeExecResult.builder().success(false).errorMsg("HTTP 请求失败: " + e.getMessage()).build();
        }
    }

    private String resolveVariables(String template, Map<String, Object> variables) {
        if (variables == null || variables.isEmpty()) return template;
        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            if (entry.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nested = (Map<String, Object>) entry.getValue();
                for (Map.Entry<String, Object> ne : nested.entrySet()) {
                    result = result.replace("{{" + entry.getKey() + "." + ne.getKey() + "}}",
                            ne.getValue() != null ? ne.getValue().toString() : "");
                }
            }
            result = result.replace("{{" + entry.getKey() + "}}",
                    entry.getValue() != null ? entry.getValue().toString() : "");
        }
        return result;
    }
}
