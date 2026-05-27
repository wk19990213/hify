package com.hify.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hify.agent.dto.AgentResponse;
import com.hify.chat.entity.ChatMessageEntity;
import com.hify.chat.mapper.ChatMessageMapper;
import com.hify.chat.service.MessageBuilder;
import com.hify.common.util.LogSanitizer;
import com.hify.knowledge.dto.RagResp;
import com.hify.knowledge.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageBuilderImpl implements MessageBuilder {

    private static final int MAX_ROUNDS = 20;

    private final ChatMessageMapper messageMapper;
    private final KnowledgeService knowledgeService;

    @Override
    public List<Map<String, Object>> buildMessages(Long sessionId, AgentResponse agent, String userContent) {
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

    @Override
    public String enrichPromptWithRag(AgentResponse agent, String userContent, String systemPrompt) {
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
}
