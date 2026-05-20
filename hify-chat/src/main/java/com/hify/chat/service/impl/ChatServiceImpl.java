package com.hify.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hify.agent.dto.AgentResponse;
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

        List<Map<String, String>> messages = buildMessages(sessionId, ctx.agent, req.getContent());

        // 保存用户消息
        ChatMessageEntity userMsg = new ChatMessageEntity();
        userMsg.setSessionId(sessionId);
        userMsg.setRole("user");
        userMsg.setContent(req.getContent());
        userMsg.setTokenCount(0);
        messageMapper.insert(userMsg);
        autoTitle(session, req.getContent());

        // 调用 LLM
        ChatRequest chatReq = new ChatRequest(ctx.modelId, messages,
                ctx.agent.getTemperature() != null ? ctx.agent.getTemperature().doubleValue() : 0.7, false);
        String llmResponse = circuitBreakerService.executeWithProtection(
                ctx.provider.getCode(),
                () -> ctx.adapter.chat(ctx.provider.getBaseUrl(), ctx.authConfig, chatReq)
        );

        int latency = (int) (System.currentTimeMillis() - start);
        String content = ctx.adapter.extractContent(llmResponse);
        String finishReason = ctx.adapter.extractFinishReason(llmResponse);
        int tokenCount = ctx.adapter.extractTokenCount(llmResponse);

        // 保存助手消息
        ChatMessageEntity assistantMsg = new ChatMessageEntity();
        assistantMsg.setSessionId(sessionId);
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(content);
        assistantMsg.setTokenCount(tokenCount);
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
        List<Map<String, String>> messages = buildMessages(sessionId, ctx.agent, req.getContent());

        // 保存用户消息（事务内，SseEmitter 之前）
        ChatMessageEntity userMsg = new ChatMessageEntity();
        userMsg.setSessionId(sessionId);
        userMsg.setRole("user");
        userMsg.setContent(req.getContent());
        userMsg.setTokenCount(0);
        messageMapper.insert(userMsg);
        autoTitle(session, req.getContent());

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        ChatRequest chatReq = new ChatRequest(ctx.modelId, messages,
                ctx.agent.getTemperature() != null ? ctx.agent.getTemperature().doubleValue() : 0.7, true);

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
    private List<Map<String, String>> buildMessages(Long sessionId, AgentResponse agent, String userContent) {
        List<Map<String, String>> messages = new ArrayList<>();

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

        messages.add(Map.of("role", "system", "content", systemPrompt));

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
            messages.add(Map.of("role", msg.getRole(), "content", msg.getContent() != null ? msg.getContent() : ""));
        }

        messages.add(Map.of("role", "user", "content", userContent));
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

    /** 内部配置聚合 */
    private record AgentContext(
            AgentResponse agent,
            String modelId,
            ProviderEntity provider,
            ProviderAdapter adapter,
            Map<String, Object> authConfig
    ) {}
}
