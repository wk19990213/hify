package com.hify.common.http;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.ConnectionPool;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class LlmHttpClient {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient syncClient;
    private final OkHttpClient streamClient;

    public LlmHttpClient() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(
                msg -> log.debug("OkHttp: {}", msg)
        );
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.HEADERS);

        this.syncClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(20, 5, TimeUnit.MINUTES))
                .addInterceptor(loggingInterceptor)
                .build();

        this.streamClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(10, 5, TimeUnit.MINUTES))
                .build();
    }

    /**
     * 同步 POST 请求，返回响应体字符串
     */
    public String post(String url, Map<String, String> headers, String body) {
        long start = System.currentTimeMillis();
        Request request = buildRequest(url, headers, body);
        try (Response response = syncClient.newCall(request).execute()) {
            long duration = System.currentTimeMillis() - start;
            int status = response.code();
            String responseBody = response.body() != null ? response.body().string() : "";
            log.info("LLM POST {} -> {} ({}ms)", url, status, duration);

            if (!response.isSuccessful()) {
                throw mapException(status, responseBody);
            }
            return responseBody;
        } catch (LlmApiException e) {
            throw e;
        } catch (SocketTimeoutException e) {
            long duration = System.currentTimeMillis() - start;
            log.error("LLM POST {} timeout ({}ms)", url, duration);
            throw new LlmApiException(LlmApiException.TIMEOUT, "请求超时: " + url, e);
        } catch (IOException e) {
            long duration = System.currentTimeMillis() - start;
            log.error("LLM POST {} failed ({}ms): {}", url, duration, e.getMessage());
            throw new LlmApiException(LlmApiException.API_ERROR, "请求失败: " + e.getMessage(), e);
        }
    }

    /**
     * 流式 SSE POST 请求，通过回调逐行返回
     */
    public void stream(String url, Map<String, String> headers, String body, StreamCallback callback) {
        long start = System.currentTimeMillis();
        Request request = buildRequest(url, headers, body);
        streamClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                long duration = System.currentTimeMillis() - start;
                if (e instanceof SocketTimeoutException) {
                    log.error("SSE stream {} timeout ({}ms)", url, duration);
                    callback.onError(new LlmApiException(LlmApiException.TIMEOUT, "SSE 连接超时", e));
                } else {
                    log.error("SSE stream {} failed ({}ms): {}", url, duration, e.getMessage());
                    callback.onError(new LlmApiException(LlmApiException.API_ERROR, "SSE 连接失败: " + e.getMessage(), e));
                }
            }

            @Override
            public void onResponse(Call call, Response response) {
                long duration = System.currentTimeMillis() - start;
                int status = response.code();
                log.info("LLM SSE {} -> {} ({}ms)", url, status, duration);

                if (!response.isSuccessful()) {
                    String errorBody = "";
                    try {
                        if (response.body() != null) {
                            errorBody = response.body().string();
                        }
                    } catch (IOException ignored) {
                    }
                    response.close();
                    callback.onError(mapException(status, errorBody));
                    return;
                }

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.body().byteStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6);
                            if (!"[DONE]".equals(data)) {
                                callback.onLine(data);
                            }
                        }
                    }
                    callback.onComplete();
                } catch (IOException e) {
                    log.error("SSE stream {} read error: {}", url, e.getMessage());
                    callback.onError(new LlmApiException(LlmApiException.API_ERROR, "SSE 读取中断", e));
                }
            }
        });
    }

    private Request buildRequest(String url, Map<String, String> headers, String body) {
        Request.Builder builder = new Request.Builder().url(url);
        if (headers != null) {
            headers.forEach(builder::addHeader);
        }
        RequestBody requestBody = body != null
                ? RequestBody.create(body, JSON)
                : RequestBody.create("", JSON);
        return builder.post(requestBody).build();
    }

    /**
     * 同步 GET 请求，返回响应体字符串
     */
    public String get(String url, Map<String, String> headers, int timeoutMs) {
        long start = System.currentTimeMillis();
        OkHttpClient client = this.syncClient.newBuilder()
                .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .build();
        Request.Builder builder = new Request.Builder().url(url).get();
        if (headers != null) {
            headers.forEach(builder::addHeader);
        }
        Request request = builder.build();
        try (Response response = client.newCall(request).execute()) {
            long duration = System.currentTimeMillis() - start;
            int status = response.code();
            String responseBody = response.body() != null ? response.body().string() : "";
            log.info("LLM GET {} -> {} ({}ms)", url, status, duration);

            if (!response.isSuccessful()) {
                throw mapException(status, responseBody);
            }
            return responseBody;
        } catch (LlmApiException e) {
            throw e;
        } catch (SocketTimeoutException e) {
            long duration = System.currentTimeMillis() - start;
            log.error("LLM GET {} timeout ({}ms)", url, duration);
            throw new LlmApiException(LlmApiException.TIMEOUT, "请求超时: " + url, e);
        } catch (IOException e) {
            long duration = System.currentTimeMillis() - start;
            log.error("LLM GET {} failed ({}ms): {}", url, duration, e.getMessage());
            throw new LlmApiException(LlmApiException.API_ERROR, "请求失败: " + e.getMessage(), e);
        }
    }

    private LlmApiException mapException(int status, String responseBody) {
        switch (status) {
            case 401:
            case 403:
                return new LlmApiException(LlmApiException.AUTH_FAILED, status, "认证失败: " + responseBody);
            case 429:
                return new LlmApiException(LlmApiException.RATE_LIMITED, status, "请求限流: " + responseBody);
            default:
                return new LlmApiException(LlmApiException.API_ERROR, status,
                        "API 错误(" + status + "): " + responseBody);
        }
    }
}
