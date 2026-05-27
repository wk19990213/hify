package com.hify.chat.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.hify.chat.service.ChatOrchestrator;
import com.hify.chat.service.ChatResult;
import com.hify.chat.service.ChatService;
import com.hify.chat.service.MessageBuilder;
import com.hify.chat.service.SessionManager;
import com.hify.chat.service.StreamEventHandler;
import com.hify.mcp.mcp.McpClientManager;
import com.hify.mcp.mcp.ToolDef;
import com.hify.workflow.dto.WorkflowInstanceResp;
import com.hify.workflow.dto.WorkflowRunReq;
import com.hify.workflow.service.WorkflowService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import jakarta.annotation.PreDestroy;
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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** ChatService Facade — 对话编排委托 ChatOrchestrator，会话/工作流自行处理。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
    private static final long SSE_TIMEOUT_MS = 120_000L;
    private final SessionManager sessionManager;
    private final AgentContextResolver agentContextResolver;
    private final MessageBuilder messageBuilder;
    private final ChatMessageMapper messageMapper;
    private final AgentService agentService;
    private final McpClientManager mcpClientManager;
    private final WorkflowService workflowService;
    private final ChatOrchestrator orchestrator;
    private final ObjectMapper objectMapper;
    private final ThreadPoolExecutor workflowExecutor = new ThreadPoolExecutor(
            2, 4, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(100),
            new ThreadFactoryBuilder().setNameFormat("workflow-pool-%d").build(),
            new ThreadPoolExecutor.CallerRunsPolicy());

    @PreDestroy void destroy() {
        workflowExecutor.shutdown();
        try { if (!workflowExecutor.awaitTermination(30, TimeUnit.SECONDS)) workflowExecutor.shutdownNow(); }
        catch (InterruptedException e) { workflowExecutor.shutdownNow(); Thread.currentThread().interrupt(); }
    }
    @Override @Transactional public ChatSessionResp createSession(Long agentId, String title) { return sessionManager.createSession(agentId, title); }
    @Override public ChatSessionResp getSessionDetail(Long sessionId) { return sessionManager.getSessionDetail(sessionId); }
    @Override public List<ChatSessionResp> listAgentSessions(Long agentId) { return sessionManager.listAgentSessions(agentId); }
    @Override @Transactional public void endSession(Long sessionId) { sessionManager.endSession(sessionId); }
    @Override @Transactional public void deleteSession(Long sessionId) { sessionManager.deleteSession(sessionId); }
    @Override public List<ChatMessageResp> getHistory(Long sessionId) { return sessionManager.getHistory(sessionId); }

    @Override @Transactional
    public ChatMessageResp sendMessage(Long sessionId, SendMessageReq req) {
        long start = System.currentTimeMillis();
        ChatSessionEntity session = sessionManager.getSession(sessionId);
        AgentContext ctx = agentContextResolver.resolveContext(session);
        if (ctx.agent().getWorkflowId() != null)
            return handleWorkflow(sessionId, session, ctx, req.getContent());
        List<ToolDef> tools = resolveTools(ctx.agent().getId());
        List<Map<String, Object>> messages = messageBuilder.buildMessages(sessionId, ctx.agent(), req.getContent());
        saveUser(sessionId, session, req.getContent());
        ChatResult result = orchestrator.execute(ctx, tools, messages);
        return saveAssistant(sessionId, result.getContent(), result.getFinishReason(),
                result.getTokenCount(), (int) (System.currentTimeMillis() - start));
    }

    @Override
    public SseEmitter sendMessageStream(Long sessionId, SendMessageReq req) {
        long start = System.currentTimeMillis();
        ChatSessionEntity session = sessionManager.getSession(sessionId);
        AgentContext ctx = agentContextResolver.resolveContext(session);
        if (ctx.agent().getWorkflowId() != null)
            return handleWorkflowStream(sessionId, session, ctx, req);
        List<ToolDef> tools = resolveTools(ctx.agent().getId());
        List<Map<String, Object>> messages = messageBuilder.buildMessages(sessionId, ctx.agent(), req.getContent());
        saveUser(sessionId, session, req.getContent());
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        StringBuilder fullContent = new StringBuilder();
        boolean[] alive = {true};
        orchestrator.executeStream(ctx, tools, messages, new StreamEventHandler() {
            public boolean onDelta(String delta) {
                if (!alive[0]) return false;
                fullContent.append(delta);
                try { emitter.send(SseEmitter.event().data(delta)); return true; }
                catch (IOException e) { alive[0] = false; return false; }
            }
            public void onComplete() {
                saveAssistant(sessionId, fullContent.toString(), "stop", 0,
                        (int) (System.currentTimeMillis() - start));
                try { emitter.send(SseEmitter.event().data("[DONE]")); } catch (IOException ignored) {}
                emitter.complete();
            }
            public void onError(Throwable t) {
                log.error("SSE stream error for session {}", sessionId, t);
                String partial = fullContent.toString();
                if (!partial.isEmpty()) saveAssistant(sessionId, partial, "stop", 0, (int) (System.currentTimeMillis() - start));
                emitter.completeWithError(t);
            }
        });
        emitter.onTimeout(() -> { log.warn("SSE timeout for session {}", sessionId); alive[0] = false; emitter.complete(); });
        emitter.onError(t -> { log.warn("SSE client disconnect for session {}", sessionId); alive[0] = false; });
        return emitter;
    }
    private ChatMessageResp handleWorkflow(Long sessionId, ChatSessionEntity session,
                                           AgentContext ctx, String userContent) {
        WorkflowRunReq runReq = new WorkflowRunReq();
        runReq.setInput(Map.of("user_message", userContent, "session_id", sessionId));
        runReq.setSessionId(sessionId); runReq.setModelConfigId(ctx.agent().getModelConfigId());
        runReq.setTools(resolveTools(ctx.agent().getId()));
        WorkflowInstanceResp wfResp = workflowService.run(ctx.agent().getWorkflowId(), runReq);
        saveUser(sessionId, session, userContent);
        return saveAssistant(sessionId, extractWorkflowOutput(wfResp.getOutputJson()), "stop", 0, 0);
    }
    private SseEmitter handleWorkflowStream(Long sessionId, ChatSessionEntity session,
                                            AgentContext ctx, SendMessageReq req) {
        saveUser(sessionId, session, req.getContent());
        SseEmitter wfEmitter = new SseEmitter(SSE_TIMEOUT_MS);
        WorkflowRunReq runReq = new WorkflowRunReq();
        runReq.setInput(Map.of("user_message", req.getContent(), "session_id", sessionId));
        runReq.setSessionId(sessionId); runReq.setModelConfigId(ctx.agent().getModelConfigId());
        runReq.setTools(resolveTools(ctx.agent().getId()));
        final Long workflowId = ctx.agent().getWorkflowId();
        workflowExecutor.execute(() -> {
            try {
                WorkflowInstanceResp wfResp = workflowService.run(workflowId, runReq);
                String output = extractWorkflowOutput(wfResp.getOutputJson());
                wfEmitter.send(SseEmitter.event().data(output));
                wfEmitter.send(SseEmitter.event().data("[DONE]"));
                ChatMessageEntity msg = new ChatMessageEntity();
                msg.setSessionId(sessionId); msg.setRole("assistant"); msg.setContent(output); msg.setTokenCount(0);
                messageMapper.insert(msg);
                wfEmitter.complete();
            } catch (Exception e) { wfEmitter.completeWithError(e); }
        });
        return wfEmitter;
    }
    private void saveUser(Long sessionId, ChatSessionEntity session, String content) {
        ChatMessageEntity msg = new ChatMessageEntity();
        msg.setSessionId(sessionId); msg.setRole("user"); msg.setContent(content); msg.setTokenCount(0);
        messageMapper.insert(msg);
        sessionManager.autoTitle(session, content);
    }
    private ChatMessageResp saveAssistant(Long sessionId, String content, String finishReason,
                                          int tokenCount, int latencyMs) {
        ChatMessageEntity msg = new ChatMessageEntity();
        msg.setSessionId(sessionId); msg.setRole("assistant"); msg.setContent(content);
        msg.setTokenCount(tokenCount); msg.setFinishReason(finishReason); msg.setLatencyMs(latencyMs);
        messageMapper.insert(msg);
        ChatMessageResp resp = new ChatMessageResp(); BeanUtils.copyProperties(msg, resp);
        return resp;
    }
    @SuppressWarnings("unchecked")
    private String extractWorkflowOutput(String outputJson) {
        if (outputJson == null || outputJson.isBlank()) return "工作流执行完成";
        try {
            Map<String, Object> output = objectMapper.readValue(outputJson, Map.class);
            if (output.containsKey("content") && output.get("content") != null) return output.get("content").toString();
            if (output.containsKey("body") && output.get("body") != null) return output.get("body").toString();
            if (output.containsKey("result") && output.get("result") != null)
                return "条件判断结果: " + Boolean.parseBoolean(output.get("result").toString());
            if (output.containsKey("sources") && output.get("sources") != null) {
                Object sources = output.get("sources");
                if (sources instanceof List) return "检索到 " + ((List<?>) sources).size() + " 条参考资料";
                return sources.toString();
            }
            return outputJson;
        } catch (Exception e) { return outputJson; }
    }
    private List<ToolDef> resolveTools(Long agentId) {
        List<ToolDef> tools = new ArrayList<>();
        List<Long> serverIds = agentService.getAgentMcpServerIds(agentId);
        if (serverIds != null) for (Long sid : serverIds) tools.addAll(mcpClientManager.listTools(sid));
        List<AgentToolResponse> atList = agentService.getAgentTools(agentId);
        if (atList != null) for (AgentToolResponse at : atList)
            if ("mcp".equals(at.getToolType()) && at.getMcpServerId() != null) tools.addAll(mcpClientManager.listTools(at.getMcpServerId()));
        return tools.isEmpty() ? null : tools;
    }
}
