package com.hify.chat.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hify.chat.dto.AgentContext;
import com.hify.chat.service.ChatOrchestrator;
import com.hify.chat.service.ChatResult;
import com.hify.chat.service.StreamEventHandler;
import com.hify.common.resilience.CircuitBreakerService;
import com.hify.mcp.mcp.ToolDef;
import com.hify.provider.adapter.ChatRequest;
import com.hify.provider.adapter.ProviderAdapter;
import com.hify.provider.service.ToolCallHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * LLM 调用编排器 — 同步/流式 LLM 调用、Function Calling 工具循环。
 * 不做消息持久化、上下文解析、工具解析、工作流处理。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatOrchestratorImpl implements ChatOrchestrator {

    private final CircuitBreakerService circuitBreaker;
    private final ToolCallHandler toolCallHandler;
    private final ObjectMapper objectMapper;

    @Override
    public ChatResult execute(AgentContext ctx, List<ToolDef> tools, List<Map<String, Object>> messages) {
        double temperature = temp(ctx);
        String content = null;
        String finishReason = "stop";
        int totalTokens = 0;

        for (int round = 0; round < 3; round++) {
            ChatRequest chatReq = new ChatRequest(ctx.modelId(), messages, temperature, false, tools);
            String llmResponse = circuitBreaker.executeWithProtection(ctx.provider().getCode(),
                    () -> ctx.adapter().chat(ctx.provider().getBaseUrl(), ctx.authConfig(), chatReq));
            totalTokens += ctx.adapter().extractTokenCount(llmResponse);
            content = ctx.adapter().extractContent(llmResponse);
            finishReason = ctx.adapter().extractFinishReason(llmResponse);

            List<ProviderAdapter.ToolCall> toolCalls = ctx.adapter().extractToolCalls(llmResponse);
            if (toolCalls == null || toolCalls.isEmpty() || tools == null) break;

            executeToolCalls(ctx.adapter(), llmResponse, toolCalls, tools, messages);
            content = null;
        }

        if (content == null) {
            content = "抱歉，工具调用未能在规定轮数内完成。";
        }
        return new ChatResult(content, finishReason, totalTokens);
    }

    @Override
    public void executeStream(AgentContext ctx, List<ToolDef> tools, List<Map<String, Object>> messages,
                              StreamEventHandler handler) {
        double temperature = temp(ctx);

        // 先执行 Function Calling 循环（同步），再流式返回最终结果
        if (tools != null && !tools.isEmpty()) {
            for (int round = 0; round < 3; round++) {
                ChatRequest toolChatReq = new ChatRequest(ctx.modelId(), messages, temperature, false, tools);
                String respBody = circuitBreaker.executeWithProtection(ctx.provider().getCode(),
                        () -> ctx.adapter().chat(ctx.provider().getBaseUrl(), ctx.authConfig(), toolChatReq));
                List<ProviderAdapter.ToolCall> toolCalls = ctx.adapter().extractToolCalls(respBody);
                if (toolCalls == null || toolCalls.isEmpty()) {
                    String assistantContent = ctx.adapter().extractContent(respBody);
                    if (assistantContent != null && !assistantContent.isEmpty()) {
                        messages.add(Map.of("role", "assistant", "content", assistantContent));
                        if (!handler.onDelta(assistantContent)) return;
                        handler.onComplete();
                        return;
                    }
                    break;
                }
                executeToolCalls(ctx.adapter(), respBody, toolCalls, tools, messages);
            }
        }

        // 流式推送最终回复
        ChatRequest chatReq = new ChatRequest(ctx.modelId(), messages, temperature, true,
                tools != null && !tools.isEmpty() ? tools : null);
        Call[] callHolder = new Call[1];

        callHolder[0] = ctx.adapter().streamChat(ctx.provider().getBaseUrl(), ctx.authConfig(), chatReq,
                new com.hify.common.http.StreamCallback() {
                    @Override
                    public void onLine(String line) {
                        String delta = ctx.adapter().extractDelta(line);
                        if (delta != null && !delta.isEmpty()) {
                            if (!handler.onDelta(delta) && callHolder[0] != null) {
                                callHolder[0].cancel();
                            }
                        }
                    }

                    @Override
                    public void onComplete() {
                        handler.onComplete();
                    }

                    @Override
                    public void onError(Throwable t) {
                        log.error("SSE stream error", t);
                        handler.onError(t);
                    }
                });
    }

    private void executeToolCalls(ProviderAdapter adapter, String llmResponse,
                                   List<ProviderAdapter.ToolCall> toolCalls, List<ToolDef> tools,
                                   List<Map<String, Object>> messages) {
        toolCallHandler.executeToolCalls(adapter, llmResponse, toolCalls, tools, messages,
                extractReasoning(llmResponse));
    }

    private String extractReasoning(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.get("choices");
            if (choices != null && choices.size() > 0) {
                JsonNode message = choices.get(0).get("message");
                if (message == null) message = choices.get(0).get("delta");
                if (message != null && message.has("reasoning_content")) {
                    return message.get("reasoning_content").asText();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static double temp(AgentContext ctx) {
        return ctx.agent().getTemperature() != null ? ctx.agent().getTemperature().doubleValue() : 0.7;
    }
}
