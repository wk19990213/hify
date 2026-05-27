package com.hify.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hify.agent.dto.AgentResponse;
import com.hify.agent.dto.AgentToolResponse;
import com.hify.agent.service.AgentService;
import com.hify.chat.dto.AgentContext;
import com.hify.chat.dto.ChatMessageResp;
import com.hify.chat.dto.ChatSessionResp;
import com.hify.chat.dto.SendMessageReq;
import com.hify.chat.entity.ChatMessageEntity;
import com.hify.chat.entity.ChatSessionEntity;
import com.hify.chat.mapper.ChatMessageMapper;
import com.hify.chat.service.AgentContextResolver;
import com.hify.chat.service.ChatService;
import com.hify.chat.service.SessionManager;
import com.hify.common.exception.BizException;
import com.hify.common.http.StreamCallback;
import com.hify.common.resilience.CircuitBreakerService;
import com.hify.common.util.LogSanitizer;
import com.hify.knowledge.dto.RagResp;
import com.hify.knowledge.service.KnowledgeService;
import com.hify.mcp.mcp.McpClientManager;
import com.hify.mcp.mcp.ToolDef;
import com.hify.provider.adapter.ProviderAdapter;
import com.hify.provider.adapter.ProviderAdapter.ToolCall;
import com.hify.provider.adapter.ChatRequest;
import com.hify.provider.service.ToolCallHandler;
import com.hify.workflow.dto.WorkflowInstanceResp;
import com.hify.workflow.dto.WorkflowRunReq;
import com.hify.workflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import jakarta.annotation.PreDestroy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final int MAX_ROUNDS = 20;
    private static final long SSE_TIMEOUT_MS = 120_000L;

    private final SessionManager sessionManager;
    private final AgentContextResolver agentContextResolver;
    private final ChatMessageMapper messageMapper;
    private final AgentService agentService;
    private final CircuitBreakerService circuitBreakerService;
    private final KnowledgeService knowledgeService;
    private final WorkflowService workflowService;
    private final McpClientManager mcpClientManager;
    private final ToolCallHandler toolCallHandler;
    private final ObjectMapper objectMapper;

    private final ThreadPoolExecutor workflowExecutor = new ThreadPoolExecutor(
            2, 4, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            new ThreadFactoryBuilder().setNameFormat("workflow-pool-%d").build(),
            new ThreadPoolExecutor.CallerRunsPolicy());

    @PreDestroy
    void destroy() {
        workflowExecutor.shutdown();
        try {
            if (!workflowExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                workflowExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            workflowExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // ==================== 会话管理 ====================

    @Override
    @Transactional
    public ChatSessionResp createSession(Long agentId, String title) {
        return sessionManager.createSession(agentId, title);
    }

    @Override
    public ChatSessionResp getSessionDetail(Long sessionId) {
        return sessionManager.getSessionDetail(sessionId);
    }

    @Override
    public List<ChatSessionResp> listAgentSessions(Long agentId) {
        return sessionManager.listAgentSessions(agentId);
    }

    @Override
    @Transactional
    public void endSession(Long sessionId) {
        sessionManager.endSession(sessionId);
    }

    @Override
    @Transactional
    public void deleteSession(Long sessionId) {
        sessionManager.deleteSession(sessionId);
    }

    // ==================== 非流式对话 ====================

    @Override
    @Transactional
    public ChatMessageResp sendMessage(Long sessionId, SendMessageReq req) {
        long start = System.currentTimeMillis();
        ChatSessionEntity session = sessionManager.getSession(sessionId);
        AgentContext ctx = agentContextResolver.resolveContext(session);

        // 检查 Agent 是否绑定了工作流
        java.util.Optional<ChatMessageResp> wfResult = handleWorkflowExecution(
                sessionId, session, ctx, req.getContent());
        if (wfResult.isPresent()) return wfResult.get();

        List<Map<String, Object>> messages = buildMessages(sessionId, ctx.agent(), req.getContent());

        // 保存用户消息
        ChatMessageEntity userMsg = saveUserMessage(sessionId, session, req.getContent());

        // 调用 LLM（支持 Function Calling）
        List<ToolDef> tools = resolveAgentTools(ctx.agent().getId());
        double temperature = ctx.agent().getTemperature() != null
                ? ctx.agent().getTemperature().doubleValue() : 0.7;

        String content = null;
        String finishReason = "stop";
        int totalTokens = 0;

        for (int round = 0; round < 3; round++) {
            ChatRequest chatReq = new ChatRequest(ctx.modelId(), messages, temperature, false,
                    tools != null ? tools : null);
            String llmResponse = circuitBreakerService.executeWithProtection(
                    ctx.provider().getCode(),
                    () -> ctx.adapter().chat(ctx.provider().getBaseUrl(), ctx.authConfig(), chatReq)
            );
            totalTokens += ctx.adapter().extractTokenCount(llmResponse);

            content = ctx.adapter().extractContent(llmResponse);
            finishReason = ctx.adapter().extractFinishReason(llmResponse);
            List<ToolCall> toolCalls = ctx.adapter().extractToolCalls(llmResponse);
            if (toolCalls == null || toolCalls.isEmpty() || tools == null) {
                break;
            }
            executeToolCalls(ctx.adapter(), llmResponse, toolCalls, tools, messages);
            content = null; // 仍需下一轮
        }

        // 如果 3 轮后仍为 null，取最后一条 assistant 消息
        if (content == null) {
            content = "抱歉，工具调用未能在规定轮数内完成。";
        }

        int latency = (int) (System.currentTimeMillis() - start);

        // 保存助手消息
        ChatMessageEntity assistantMsg = new ChatMessageEntity();
        assistantMsg.setSessionId(sessionId);
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(content);
        assistantMsg.setTokenCount(totalTokens);
        assistantMsg.setFinishReason(finishReason);
        assistantMsg.setLatencyMs(latency);
        messageMapper.insert(assistantMsg);

        return toMessageResp(assistantMsg);
    }

    // ==================== 流式对话 ====================

    @Override
    public SseEmitter sendMessageStream(Long sessionId, SendMessageReq req) {
        long start = System.currentTimeMillis();
        ChatSessionEntity session = sessionManager.getSession(sessionId);
        AgentContext ctx = agentContextResolver.resolveContext(session);

        // 检查 Agent 是否绑定了工作流
        if (ctx.agent().getWorkflowId() != null) {
            return handleWorkflowStream(sessionId, session, ctx, req);
        }

        List<Map<String, Object>> messages = buildMessages(sessionId, ctx.agent(), req.getContent());

        // 保存用户消息（事务内，SseEmitter 之前）
        saveUserMessage(sessionId, session, req.getContent());

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        // 先执行 Function Calling 循环（同步），再流式返回最终结果
        List<ToolDef> tools = resolveAgentTools(ctx.agent().getId());
        double temperature = ctx.agent().getTemperature() != null
                ? ctx.agent().getTemperature().doubleValue() : 0.7;

        if (tools != null && !tools.isEmpty()) {
            for (int round = 0; round < 3; round++) {
                ChatRequest toolChatReq = new ChatRequest(ctx.modelId(), messages, temperature, false,
                        tools);
                String respBody = circuitBreakerService.executeWithProtection(
                        ctx.provider().getCode(),
                        () -> ctx.adapter().chat(ctx.provider().getBaseUrl(), ctx.authConfig(), toolChatReq)
                );
                List<ToolCall> toolCalls = ctx.adapter().extractToolCalls(respBody);
                if (toolCalls == null || toolCalls.isEmpty()) {
                    String assistantContent = ctx.adapter().extractContent(respBody);
                    if (assistantContent != null && !assistantContent.isEmpty()) {
                        messages.add(Map.<String, Object>of("role", "assistant", "content", assistantContent));
                    }
                    try {
                        emitter.send(SseEmitter.event().data(
                                assistantContent != null ? assistantContent : ""));
                        emitter.send(SseEmitter.event().data("[DONE]"));
                    } catch (IOException ioEx) {
                        emitter.completeWithError(ioEx);
                    }
                    emitter.complete();
                    return emitter;
                }
                executeToolCalls(ctx.adapter(), respBody, toolCalls, tools, messages);
            }
        }

        ChatRequest chatReq = new ChatRequest(ctx.modelId(), messages, temperature, true,
                tools != null && !tools.isEmpty() ? tools : null);

        // 持有 OkHttp Call 引用，用于取消
        Call[] callHolder = new Call[1];
        StringBuilder fullContent = new StringBuilder();

        callHolder[0] = ctx.adapter().streamChat(ctx.provider().getBaseUrl(), ctx.authConfig(), chatReq, new StreamCallback() {
            @Override
            public void onLine(String line) {
                String delta = ctx.adapter().extractDelta(line);
                if (delta != null && !delta.isEmpty()) {
                    fullContent.append(delta);
                    try {
                        emitter.send(SseEmitter.event().data(delta));
                    } catch (IOException e) {
                        // 客户端断开 → 取消 OkHttp 到 LLM 的请求
                        if (callHolder[0] != null) callHolder[0].cancel();
                    }
                }
            }

            @Override
            public void onComplete() {
                saveAssistantMessage(sessionId, fullContent.toString(), ctx, (int) (System.currentTimeMillis() - start));
                try { emitter.send(SseEmitter.event().data("[DONE]")); } catch (IOException ignored) {}
                emitter.complete();
            }

            @Override
            public void onError(Throwable t) {
                log.error("SSE stream error for session {}", sessionId, t);
                String partial = fullContent.toString();
                if (!partial.isEmpty()) {
                    saveAssistantMessage(sessionId, partial, ctx, (int) (System.currentTimeMillis() - start));
                }
                emitter.completeWithError(t);
            }
        });

        // 坑1：SseEmitter 超时 → 取消上游 OkHttp 请求
        emitter.onTimeout(() -> {
            log.warn("SSE timeout for session {}", sessionId);
            if (callHolder[0] != null) callHolder[0].cancel();
            emitter.complete();
        });

        // 坑2：客户端断开连接 → 取消上游 OkHttp 请求
        emitter.onError(t -> {
            log.warn("SSE client disconnect for session {}", sessionId);
            if (callHolder[0] != null) callHolder[0].cancel();
        });

        return emitter;
    }

    private SseEmitter handleWorkflowStream(Long sessionId, ChatSessionEntity session,
                                             AgentContext ctx, SendMessageReq req) {
        saveUserMessage(sessionId, session, req.getContent());

        SseEmitter wfEmitter = new SseEmitter(SSE_TIMEOUT_MS);
        Map<String, Object> workflowInput = new java.util.HashMap<>();
        workflowInput.put("user_message", req.getContent());
        workflowInput.put("session_id", sessionId);
        WorkflowRunReq runReq = new WorkflowRunReq();
        runReq.setInput(workflowInput);
        runReq.setSessionId(sessionId);
        runReq.setModelConfigId(ctx.agent().getModelConfigId());
        runReq.setTools(resolveAgentTools(ctx.agent().getId()));
        final Long workflowId = ctx.agent().getWorkflowId();

        workflowExecutor.execute(() -> {
            try {
                WorkflowInstanceResp wfResp = workflowService.run(workflowId, runReq);
                String wfOutput = extractWorkflowOutput(wfResp.getOutputJson());
                wfEmitter.send(SseEmitter.event().data(wfOutput));
                wfEmitter.send(SseEmitter.event().data("[DONE]"));

                ChatMessageEntity assistantMsg = new ChatMessageEntity();
                assistantMsg.setSessionId(sessionId);
                assistantMsg.setRole("assistant");
                assistantMsg.setContent(wfOutput);
                assistantMsg.setTokenCount(0);
                messageMapper.insert(assistantMsg);

                wfEmitter.complete();
            } catch (Exception e) {
                wfEmitter.completeWithError(e);
            }
        });

        return wfEmitter;
    }

    @Transactional
    void saveAssistantMessage(Long sessionId, String content, AgentContext ctx, int latencyMs) {
        ChatMessageEntity msg = new ChatMessageEntity();
        msg.setSessionId(sessionId);
        msg.setRole("assistant");
        msg.setContent(content);
        msg.setTokenCount(0);
        msg.setFinishReason("stop");
        msg.setLatencyMs(latencyMs);
        messageMapper.insert(msg);
    }

    // ==================== 历史消息 ====================

    @Override
    public List<ChatMessageResp> getHistory(Long sessionId) {
        return sessionManager.getHistory(sessionId);
    }

    // ==================== 内部：消息构建 ====================

    /** 构造消息列表：system prompt + 历史 + 当前用户消息 */
    private List<Map<String, Object>> buildMessages(Long sessionId, AgentResponse agent, String userContent) {
        List<Map<String, Object>> messages = new ArrayList<>();

        // 系统提示词
        String systemPrompt = agent.getSystemPrompt();
        if (systemPrompt == null || systemPrompt.isBlank()) {
            systemPrompt = "你是一个有用的AI助手。";
        }

        // RAG 检索增强
        systemPrompt = enrichPromptWithRag(agent, userContent, systemPrompt);

        messages.add(Map.<String, Object>of("role", "system", "content", systemPrompt));

        // 历史消息（受 maxRounds 限制）- 使用分页查询避免 SQL 注入
        int maxRounds = agent.getConversationMaxRounds() != null ? agent.getConversationMaxRounds() : MAX_ROUNDS;
        int historyLimit = maxRounds * 2;
        Page<ChatMessageEntity> page = Page.of(1, historyLimit);
        LambdaQueryWrapper<ChatMessageEntity> wrapper = new LambdaQueryWrapper<ChatMessageEntity>()
                .eq(ChatMessageEntity::getSessionId, sessionId)
                .eq(ChatMessageEntity::getDeleted, 0)
                .orderByDesc(ChatMessageEntity::getCreatedAt);
        Page<ChatMessageEntity> resultPage = messageMapper.selectPage(page, wrapper);
        List<ChatMessageEntity> history = resultPage.getRecords();
        for (int i = history.size() - 1; i >= 0; i--) {
            ChatMessageEntity msg = history.get(i);
            messages.add(Map.<String, Object>of("role", msg.getRole(), "content", msg.getContent() != null ? msg.getContent() : ""));
        }

        messages.add(Map.<String, Object>of("role", "user", "content", userContent));
        return messages;
    }

    /**
     * RAG 知识库检索增强系统提示词
     */
    private String enrichPromptWithRag(AgentResponse agent, String userContent, String systemPrompt) {
        if (agent.getKbId() == null) {
            return systemPrompt;
        }
        try {
            RagResp rag = knowledgeService.query(agent.getKbId(), userContent);
            if (rag.getSources() != null && !rag.getSources().isEmpty()) {
                String ctx = String.join("\n\n", rag.getSources());
                return systemPrompt + "\n\n请根据以下参考资料回答用户问题：\n" + ctx;
            }
        } catch (Exception e) {
            log.warn("RAG query failed for agent {} kb {}: {}",
                    agent.getId(), agent.getKbId(), LogSanitizer.sanitize(e.getMessage()));
        }
        return systemPrompt;
    }

    // ==================== 内部：辅助方法 ====================

    /**
     * 处理工作流执行（当 Agent 绑定了工作流时）
     * @return 如果执行了工作流则返回响应，否则返回 empty
     */
    private java.util.Optional<ChatMessageResp> handleWorkflowExecution(
            Long sessionId, ChatSessionEntity session, AgentContext ctx, String userContent) {
        if (ctx.agent().getWorkflowId() == null) {
            return java.util.Optional.empty();
        }
        Map<String, Object> workflowInput = new java.util.HashMap<>();
        workflowInput.put("user_message", userContent);
        workflowInput.put("session_id", sessionId);
        WorkflowRunReq runReq = new WorkflowRunReq();
        runReq.setInput(workflowInput);
        runReq.setSessionId(sessionId);
        runReq.setModelConfigId(ctx.agent().getModelConfigId());
        runReq.setTools(resolveAgentTools(ctx.agent().getId()));
        WorkflowInstanceResp wfResp = workflowService.run(ctx.agent().getWorkflowId(), runReq);

        saveUserMessage(sessionId, session, userContent);

        String wfOutput = extractWorkflowOutput(wfResp.getOutputJson());
        ChatMessageEntity assistantMsg = new ChatMessageEntity();
        assistantMsg.setSessionId(sessionId);
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(wfOutput);
        assistantMsg.setTokenCount(0);
        messageMapper.insert(assistantMsg);
        return java.util.Optional.of(toMessageResp(assistantMsg));
    }

    /** 保存用户消息并自动生成会话标题 */
    private ChatMessageEntity saveUserMessage(Long sessionId, ChatSessionEntity session, String content) {
        ChatMessageEntity userMsg = new ChatMessageEntity();
        userMsg.setSessionId(sessionId);
        userMsg.setRole("user");
        userMsg.setContent(content);
        userMsg.setTokenCount(0);
        messageMapper.insert(userMsg);
        sessionManager.autoTitle(session, content);
        return userMsg;
    }

    private ChatMessageResp toMessageResp(ChatMessageEntity entity) {
        ChatMessageResp resp = new ChatMessageResp();
        BeanUtils.copyProperties(entity, resp);
        return resp;
    }

    /**
     * 从工作流输出 JSON 中提取人类可读文本。
     * LLM 节点返回 content，HTTP 返回 body，Condition 返回布尔描述，RAG 返回来源数量。
     */
    @SuppressWarnings("unchecked")
    private String extractWorkflowOutput(String outputJson) {
        if (outputJson == null || outputJson.isBlank()) {
            return "工作流执行完成";
        }
        try {
            Map<String, Object> output = objectMapper.readValue(outputJson, Map.class);
            if (output.containsKey("content") && output.get("content") != null) {
                return output.get("content").toString();
            }
            if (output.containsKey("body") && output.get("body") != null) {
                return output.get("body").toString();
            }
            if (output.containsKey("result") && output.get("result") != null) {
                boolean result = Boolean.parseBoolean(output.get("result").toString());
                return "条件判断结果: " + result;
            }
            if (output.containsKey("sources") && output.get("sources") != null) {
                Object sources = output.get("sources");
                if (sources instanceof List) {
                    return "检索到 " + ((List<?>) sources).size() + " 条参考资料";
                }
                return sources.toString();
            }
            return outputJson;
        } catch (Exception e) {
            return outputJson;
        }
    }

    /** 解析 Agent 绑定的 MCP 工具列表（新旧表双读） */
    private List<ToolDef> resolveAgentTools(Long agentId) {
        List<ToolDef> tools = new ArrayList<>();

        // 新表 agent_mcp_server：按服务绑定
        List<Long> mcpServerIds = agentService.getAgentMcpServerIds(agentId);
        if (mcpServerIds != null) {
            for (Long serverId : mcpServerIds) {
                tools.addAll(mcpClientManager.listTools(serverId));
            }
        }

        // 旧表 agent_tool：按工具绑定（灰度期兼容）
        List<AgentToolResponse> agentTools = agentService.getAgentTools(agentId);
        if (agentTools != null) {
            for (AgentToolResponse at : agentTools) {
                if ("mcp".equals(at.getToolType()) && at.getMcpServerId() != null) {
                    tools.addAll(mcpClientManager.listTools(at.getMcpServerId()));
                }
            }
        }

        return tools.isEmpty() ? null : tools;
    }

    private void executeToolCalls(ProviderAdapter adapter, String llmResponse,
                                   List<ToolCall> toolCalls, List<ToolDef> tools,
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
}
