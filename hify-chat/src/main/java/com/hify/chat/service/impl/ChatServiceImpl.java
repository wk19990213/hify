package com.hify.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hify.agent.dto.AgentResponse;
import com.hify.agent.dto.AgentToolResponse;
import com.hify.agent.service.AgentService;
import com.hify.chat.dto.ChatMessageResp;
import com.hify.chat.dto.ChatSessionResp;
import com.hify.chat.dto.SendMessageReq;
import com.hify.chat.entity.ChatMessageEntity;
import com.hify.chat.entity.ChatSessionEntity;
import com.hify.chat.mapper.ChatMessageMapper;
import com.hify.chat.mapper.ChatSessionMapper;
import com.hify.chat.service.ChatService;
import com.hify.common.crypto.AesEncryptor;
import com.hify.common.exception.BizException;
import com.hify.common.http.StreamCallback;
import com.hify.common.resilience.CircuitBreakerService;
import com.hify.knowledge.dto.RagResp;
import com.hify.knowledge.service.KnowledgeService;
import com.hify.mcp.mcp.McpClientManager;
import com.hify.mcp.mcp.ToolDef;
import com.hify.mcp.mcp.ToolResult;
import com.hify.provider.adapter.ProviderAdapter.ToolCall;
import okhttp3.Call;
import com.hify.provider.adapter.ChatRequest;
import com.hify.provider.adapter.ProviderAdapter;
import com.hify.provider.adapter.ProviderAdapterFactory;
import com.hify.provider.entity.ModelConfigEntity;
import com.hify.provider.entity.ProviderEntity;
import com.hify.provider.entity.ProviderModelEntity;
import com.hify.provider.mapper.ModelConfigMapper;
import com.hify.provider.mapper.ProviderMapper;
import com.hify.provider.mapper.ProviderModelMapper;
import com.hify.workflow.dto.WorkflowInstanceResp;
import com.hify.workflow.dto.WorkflowRunReq;
import com.hify.workflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final int MAX_ROUNDS = 20;
    private static final long SSE_TIMEOUT_MS = 120_000L;

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    private final AgentService agentService;
    private final ModelConfigMapper modelConfigMapper;
    private final ProviderMapper providerMapper;
    private final ProviderModelMapper providerModelMapper;
    private final ProviderAdapterFactory adapterFactory;
    private final CircuitBreakerService circuitBreakerService;
    private final KnowledgeService knowledgeService;
    private final WorkflowService workflowService;
    private final McpClientManager mcpClientManager;
    private final ObjectMapper objectMapper;

    // ==================== 会话管理 ====================

    @Override
    @Transactional
    public ChatSessionResp createSession(Long agentId, String title) {
        agentService.getDetail(agentId); // 校验 Agent 存在
        ChatSessionEntity session = new ChatSessionEntity();
        session.setSessionId(UUID.randomUUID().toString());
        session.setAgentId(agentId);
        session.setTitle(title != null ? title : "新对话");
        session.setStatus("active");
        sessionMapper.insert(session);
        return toSessionResp(session, List.of());
    }

    @Override
    public ChatSessionResp getSessionDetail(Long sessionId) {
        ChatSessionEntity session = getSession(sessionId);
        List<ChatMessageResp> messages = getHistory(sessionId);
        return toSessionResp(session, messages);
    }

    @Override
    public List<ChatSessionResp> listAgentSessions(Long agentId) {
        List<ChatSessionEntity> sessions = sessionMapper.selectList(
                new LambdaQueryWrapper<ChatSessionEntity>()
                        .eq(ChatSessionEntity::getAgentId, agentId)
                        .eq(ChatSessionEntity::getStatus, "active")
                        .eq(ChatSessionEntity::getDeleted, 0)
                        .ge(ChatSessionEntity::getCreatedAt, java.time.LocalDateTime.now().minusMonths(1))
                        .orderByDesc(ChatSessionEntity::getCreatedAt)
        );
        return sessions.stream()
                .map(s -> toSessionResp(s, List.of()))
                .toList();
    }

    /** 取用户消息前 20 字作为会话标题（首次发送时自动设置） */
    private void autoTitle(ChatSessionEntity session, String userContent) {
        if (!"新对话".equals(session.getTitle())) return;
        String title = userContent.length() > 20 ? userContent.substring(0, 20) + "..." : userContent;
        // 去掉换行
        title = title.replace('\n', ' ').replace('\r', ' ');
        session.setTitle(title);
        sessionMapper.updateById(session);
    }

    @Override
    @Transactional
    public void endSession(Long sessionId) {
        ChatSessionEntity session = getSession(sessionId);
        session.setStatus("ended");
        sessionMapper.updateById(session);
    }

    @Override
    @Transactional
    public void deleteSession(Long sessionId) {
        ChatSessionEntity session = getSession(sessionId);
        // 删除会话及其所有消息
        messageMapper.delete(new LambdaQueryWrapper<ChatMessageEntity>()
                .eq(ChatMessageEntity::getSessionId, sessionId));
        sessionMapper.deleteById(sessionId);
    }

    // ==================== 非流式对话 ====================

    @Override
    @Transactional
    public ChatMessageResp sendMessage(Long sessionId, SendMessageReq req) {
        long start = System.currentTimeMillis();
        ChatSessionEntity session = getSession(sessionId);
        AgentContext ctx = resolveContext(session);

        // 检查 Agent 是否绑定了工作流
        if (ctx.agent.getWorkflowId() != null) {
            Map<String, Object> workflowInput = new java.util.HashMap<>();
            workflowInput.put("user_message", req.getContent());
            workflowInput.put("session_id", sessionId);
            WorkflowRunReq runReq = new WorkflowRunReq();
            runReq.setInput(workflowInput);
            runReq.setSessionId(sessionId);
            runReq.setModelConfigId(ctx.agent.getModelConfigId());
            runReq.setTools(resolveAgentTools(ctx.agent.getId()));
            WorkflowInstanceResp wfResp = workflowService.run(ctx.agent.getWorkflowId(), runReq);

            // 保存用户消息
            ChatMessageEntity userMsg = new ChatMessageEntity();
            userMsg.setSessionId(sessionId);
            userMsg.setRole("user");
            userMsg.setContent(req.getContent());
            userMsg.setTokenCount(0);
            messageMapper.insert(userMsg);
            autoTitle(session, req.getContent());

            // 保存助手消息（工作流输出）
            String wfOutput = extractWorkflowOutput(wfResp.getOutputJson());
            ChatMessageEntity assistantMsg = new ChatMessageEntity();
            assistantMsg.setSessionId(sessionId);
            assistantMsg.setRole("assistant");
            assistantMsg.setContent(wfOutput);
            assistantMsg.setTokenCount(0);
            messageMapper.insert(assistantMsg);
            return toMessageResp(assistantMsg);
        }

        List<Map<String, Object>> messages = buildMessages(sessionId, ctx.agent, req.getContent());

        // 保存用户消息
        ChatMessageEntity userMsg = new ChatMessageEntity();
        userMsg.setSessionId(sessionId);
        userMsg.setRole("user");
        userMsg.setContent(req.getContent());
        userMsg.setTokenCount(0);
        messageMapper.insert(userMsg);
        autoTitle(session, req.getContent());

        // 调用 LLM（支持 Function Calling）
        List<ToolDef> tools = resolveAgentTools(ctx.agent.getId());
        double temperature = ctx.agent.getTemperature() != null
                ? ctx.agent.getTemperature().doubleValue() : 0.7;

        String content = null;
        String finishReason = "stop";
        int totalTokens = 0;

        for (int round = 0; round < 3; round++) {
            ChatRequest chatReq = new ChatRequest(ctx.modelId, messages, temperature, false,
                    tools != null ? tools : null);
            String llmResponse = circuitBreakerService.executeWithProtection(
                    ctx.provider.getCode(),
                    () -> ctx.adapter.chat(ctx.provider.getBaseUrl(), ctx.authConfig, chatReq)
            );
            totalTokens += ctx.adapter.extractTokenCount(llmResponse);

            List<ToolCall> toolCalls = ctx.adapter.extractToolCalls(llmResponse);
            if (toolCalls == null || toolCalls.isEmpty() || tools == null) {
                content = ctx.adapter.extractContent(llmResponse);
                finishReason = ctx.adapter.extractFinishReason(llmResponse);
                break;
            }

            // 追加 assistant 消息（含 tool calls）
            String assistantContent = ctx.adapter.extractContent(llmResponse);
            String reasoningContent = extractReasoning(llmResponse);
            List<Map<String, Object>> toolCallMaps = new ArrayList<>();
            for (ToolCall tc : toolCalls) {
                Map<String, Object> func = new java.util.LinkedHashMap<>();
                func.put("name", tc.getName());
                func.put("arguments", toJson(tc.getArguments()));
                toolCallMaps.add(Map.of("id", tc.getId() != null ? tc.getId() : "",
                        "type", "function", "function", func));
            }
            Map<String, Object> assistantMsg = new java.util.LinkedHashMap<>();
            assistantMsg.put("role", "assistant");
            assistantMsg.put("content", assistantContent != null ? assistantContent : "");
            if (reasoningContent != null) {
                assistantMsg.put("reasoning_content", reasoningContent);
            }
            assistantMsg.put("tool_calls", toolCallMaps);
            messages.add(assistantMsg);

            // 执行工具调用并追加 tool 消息
            for (ToolCall tc : toolCalls) {
                ToolDef def = findToolDef(tools, tc.getName());
                Long serverId = def != null ? def.getServerId() : null;
                if (serverId == null) continue;
                ToolResult tr = mcpClientManager.callTool(serverId, tc.getName(), tc.getArguments());
                Map<String, Object> toolMsg = new java.util.LinkedHashMap<>();
                toolMsg.put("role", "tool");
                toolMsg.put("tool_call_id", tc.getId() != null ? tc.getId() : "");
                toolMsg.put("content", tr.isSuccess() ? tr.getContent() : "Error: " + tr.getError());
                messages.add(toolMsg);
            }
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
        ChatSessionEntity session = getSession(sessionId);
        AgentContext ctx = resolveContext(session);

        // 检查 Agent 是否绑定了工作流
        if (ctx.agent.getWorkflowId() != null) {
            // 保存用户消息
            ChatMessageEntity userMsg = new ChatMessageEntity();
            userMsg.setSessionId(sessionId);
            userMsg.setRole("user");
            userMsg.setContent(req.getContent());
            userMsg.setTokenCount(0);
            messageMapper.insert(userMsg);
            autoTitle(session, req.getContent());

            SseEmitter wfEmitter = new SseEmitter(SSE_TIMEOUT_MS);
            Map<String, Object> workflowInput = new java.util.HashMap<>();
            workflowInput.put("user_message", req.getContent());
            workflowInput.put("session_id", sessionId);
            WorkflowRunReq runReq = new WorkflowRunReq();
            runReq.setInput(workflowInput);
            runReq.setSessionId(sessionId);
            runReq.setModelConfigId(ctx.agent.getModelConfigId());
            runReq.setTools(resolveAgentTools(ctx.agent.getId()));
            final Long workflowId = ctx.agent.getWorkflowId();

            new Thread(() -> {
                try {
                    WorkflowInstanceResp wfResp = workflowService.run(workflowId, runReq);
                    String wfOutput = extractWorkflowOutput(wfResp.getOutputJson());
                    wfEmitter.send(SseEmitter.event().data(wfOutput));
                    wfEmitter.send(SseEmitter.event().data("[DONE]"));

                    // 保存助手消息
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
            }).start();

            return wfEmitter;
        }

        List<Map<String, Object>> messages = buildMessages(sessionId, ctx.agent, req.getContent());

        // 保存用户消息（事务内，SseEmitter 之前）
        ChatMessageEntity userMsg = new ChatMessageEntity();
        userMsg.setSessionId(sessionId);
        userMsg.setRole("user");
        userMsg.setContent(req.getContent());
        userMsg.setTokenCount(0);
        messageMapper.insert(userMsg);
        autoTitle(session, req.getContent());

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        // 先执行 Function Calling 循环（同步），再流式返回最终结果
        List<ToolDef> tools = resolveAgentTools(ctx.agent.getId());
        double temperature = ctx.agent.getTemperature() != null
                ? ctx.agent.getTemperature().doubleValue() : 0.7;

        if (tools != null && !tools.isEmpty()) {
            for (int round = 0; round < 3; round++) {
                ChatRequest toolChatReq = new ChatRequest(ctx.modelId, messages, temperature, false,
                        tools);
                String respBody = circuitBreakerService.executeWithProtection(
                        ctx.provider.getCode(),
                        () -> ctx.adapter.chat(ctx.provider.getBaseUrl(), ctx.authConfig, toolChatReq)
                );
                List<ToolCall> toolCalls = ctx.adapter.extractToolCalls(respBody);
                if (toolCalls == null || toolCalls.isEmpty()) {
                    // LLM 最终决定不调工具 → 直接返回文本片段即可
                    String assistantContent = ctx.adapter.extractContent(respBody);
                    if (assistantContent != null && !assistantContent.isEmpty()) {
                        messages.add(Map.<String, Object>of("role", "assistant", "content", assistantContent));
                    }
                    break;
                }
                String assistantContent = ctx.adapter.extractContent(respBody);
                String reasoningContent = extractReasoning(respBody);
                List<Map<String, Object>> tcMaps = new ArrayList<>();
                for (ToolCall tc : toolCalls) {
                    Map<String, Object> func = new java.util.LinkedHashMap<>();
                    func.put("name", tc.getName());
                    func.put("arguments", toJson(tc.getArguments()));
                    tcMaps.add(Map.of("id", tc.getId() != null ? tc.getId() : "",
                            "type", "function", "function", func));
                }
                Map<String, Object> asstMsg = new java.util.LinkedHashMap<>();
                asstMsg.put("role", "assistant");
                asstMsg.put("content", assistantContent != null ? assistantContent : "");
                if (reasoningContent != null) {
                    asstMsg.put("reasoning_content", reasoningContent);
                }
                asstMsg.put("tool_calls", tcMaps);
                messages.add(asstMsg);
                for (ToolCall tc : toolCalls) {
                    ToolDef def = findToolDef(tools, tc.getName());
                    Long serverId = def != null ? def.getServerId() : null;
                    if (serverId == null) continue;
                    ToolResult tr = mcpClientManager.callTool(serverId, tc.getName(), tc.getArguments());
                    Map<String, Object> toolMsg = new java.util.LinkedHashMap<>();
                    toolMsg.put("role", "tool");
                    toolMsg.put("tool_call_id", tc.getId() != null ? tc.getId() : "");
                    toolMsg.put("content", tr.isSuccess() ? tr.getContent() : "Error: " + tr.getError());
                    messages.add(toolMsg);
                }
            }
        }

        ChatRequest chatReq = new ChatRequest(ctx.modelId, messages, temperature, true,
                tools != null && !tools.isEmpty() ? tools : null);

        // 持有 OkHttp Call 引用，用于取消
        Call[] callHolder = new Call[1];
        StringBuilder fullContent = new StringBuilder();

        callHolder[0] = ctx.adapter.streamChat(ctx.provider.getBaseUrl(), ctx.authConfig, chatReq, new StreamCallback() {
            @Override
            public void onLine(String line) {
                String delta = ctx.adapter.extractDelta(line);
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
        return messageMapper.selectList(
                        new LambdaQueryWrapper<ChatMessageEntity>()
                                .eq(ChatMessageEntity::getSessionId, sessionId)
                                .eq(ChatMessageEntity::getDeleted, 0)
                                .orderByAsc(ChatMessageEntity::getCreatedAt))
                .stream()
                .map(this::toMessageResp)
                .toList();
    }

    // ==================== 内部：配置解析链 ====================

    /** Agent → ModelConfig → model_id → provider_model → Provider → Adapter */
    private AgentContext resolveContext(ChatSessionEntity session) {
        AgentResponse agent = agentService.getDetail(session.getAgentId());

        // 工作流 Agent 不需要模型配置
        if (agent.getWorkflowId() != null) {
            return new AgentContext(agent, null, null, null, null);
        }

        if (agent.getModelConfigId() == null) {
            throw BizException.paramError("Agent 未绑定模型配置");
        }
        ModelConfigEntity modelConfig = modelConfigMapper.selectById(agent.getModelConfigId());
        if (modelConfig == null || modelConfig.getDeleted() == 1) {
            throw BizException.notFound("模型配置不存在");
        }
        if (modelConfig.getProviderCount() == null || modelConfig.getProviderCount() <= 0) {
            throw BizException.notFound("模型没有可用提供商");
        }

        // 通过 provider_model 查找可用 Provider（优先使用 model_config 记录的 provider_id）
        List<ProviderModelEntity> pmList = providerModelMapper.selectList(
                new LambdaQueryWrapper<ProviderModelEntity>()
                        .eq(ProviderModelEntity::getModelId, modelConfig.getModelId()));

        ProviderEntity provider = null;
        for (ProviderModelEntity pm : pmList) {
            ProviderEntity p = providerMapper.selectById(pm.getProviderId());
            if (p != null && p.getDeleted() == 0 && p.getStatus() == 1) {
                if (p.getId().equals(modelConfig.getProviderId())) {
                    provider = p; // 优先使用 model_config 记录的 provider
                    break;
                }
                if (provider == null) {
                    provider = p;
                }
            }
        }

        if (provider == null) {
            throw BizException.notFound("模型的所有提供商均不可用");
        }

        ProviderAdapter adapter = adapterFactory.getAdapter(provider.getType());

        // 解密 authConfig
        Map<String, Object> authConfig = null;
        String encrypted = provider.getAuthConfig();
        if (encrypted != null && !encrypted.isEmpty()) {
            String json = AesEncryptor.decrypt(encrypted);
            try {
                authConfig = objectMapper.readValue(json, Map.class);
            } catch (Exception e) {
                log.error("Failed to parse authConfig", e);
            }
        }

        return new AgentContext(agent, modelConfig.getModelId(), provider, adapter, authConfig);
    }

    /** 构造消息列表：system prompt + 历史 + 当前用户消息 */
    private List<Map<String, Object>> buildMessages(Long sessionId, AgentResponse agent, String userContent) {
        List<Map<String, Object>> messages = new ArrayList<>();

        // 系统提示词
        String systemPrompt = agent.getSystemPrompt();
        if (systemPrompt == null || systemPrompt.isBlank()) {
            systemPrompt = "你是一个有用的AI助手。";
        }

        // RAG 检索：Agent 绑定了知识库时，检索相关上下文
        if (agent.getKbId() != null) {
            try {
                RagResp rag = knowledgeService.query(agent.getKbId(), userContent);
                if (rag.getSources() != null && !rag.getSources().isEmpty()) {
                    String ctx = String.join("\n\n", rag.getSources());
                    systemPrompt += "\n\n请根据以下参考资料回答用户问题：\n" + ctx;
                }
            } catch (Exception e) {
                log.warn("RAG query failed for agent {} kb {}: {}", agent.getId(), agent.getKbId(), e.getMessage());
            }
        }

        messages.add(Map.<String, Object>of("role", "system", "content", systemPrompt));

        // 历史消息（受 maxRounds 限制）
        int maxRounds = agent.getConversationMaxRounds() != null ? agent.getConversationMaxRounds() : MAX_ROUNDS;
        int historyLimit = maxRounds * 2;
        List<ChatMessageEntity> history = messageMapper.selectList(
                new LambdaQueryWrapper<ChatMessageEntity>()
                        .eq(ChatMessageEntity::getSessionId, sessionId)
                        .eq(ChatMessageEntity::getDeleted, 0)
                        .orderByDesc(ChatMessageEntity::getCreatedAt)
                        .last("LIMIT " + historyLimit)
        );
        for (int i = history.size() - 1; i >= 0; i--) {
            ChatMessageEntity msg = history.get(i);
            messages.add(Map.<String, Object>of("role", msg.getRole(), "content", msg.getContent() != null ? msg.getContent() : ""));
        }

        messages.add(Map.<String, Object>of("role", "user", "content", userContent));
        return messages;
    }

    // ==================== 内部：辅助方法 ====================

    private ChatSessionEntity getSession(Long sessionId) {
        ChatSessionEntity session = sessionMapper.selectById(sessionId);
        if (session == null || session.getDeleted() == 1) {
            throw BizException.notFound("会话不存在");
        }
        if ("ended".equals(session.getStatus())) {
            throw BizException.paramError("会话已结束");
        }
        return session;
    }

    private ChatMessageResp toMessageResp(ChatMessageEntity entity) {
        ChatMessageResp resp = new ChatMessageResp();
        BeanUtils.copyProperties(entity, resp);
        return resp;
    }

    private ChatSessionResp toSessionResp(ChatSessionEntity s, List<ChatMessageResp> msgs) {
        ChatSessionResp resp = new ChatSessionResp();
        resp.setSessionId(s.getId());
        resp.setSessionUuid(s.getSessionId());
        resp.setAgentId(s.getAgentId());
        resp.setTitle(s.getTitle());
        resp.setStatus(s.getStatus());
        resp.setMessages(msgs);
        resp.setCreatedAt(s.getCreatedAt());
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

    private ToolDef findToolDef(List<ToolDef> tools, String name) {
        if (tools == null) return null;
        return tools.stream().filter(t -> t.getName().equals(name)).findFirst().orElse(null);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            return "{}";
        }
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

    /** 内部配置聚合 */
    private record AgentContext(
            AgentResponse agent,
            String modelId,
            ProviderEntity provider,
            ProviderAdapter adapter,
            Map<String, Object> authConfig
    ) {}
}
