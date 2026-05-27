package com.hify.chat.service;

import com.hify.agent.dto.AgentResponse;

import java.util.List;
import java.util.Map;

/**
 * 消息构建器 —— 构造对话消息列表（system prompt + RAG 增强 + 历史 + 当前用户消息）。
 */
public interface MessageBuilder {

    /**
     * 构造消息列表：system prompt + RAG 知识库增强 + 历史消息 + 当前用户消息。
     */
    List<Map<String, Object>> buildMessages(Long sessionId, AgentResponse agent, String userContent);

    /**
     * RAG 知识库检索增强系统提示词。
     */
    String enrichPromptWithRag(AgentResponse agent, String userContent, String systemPrompt);
}
