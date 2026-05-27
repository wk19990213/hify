package com.hify.chat.dto;

import com.hify.agent.dto.AgentResponse;
import com.hify.provider.adapter.ProviderAdapter;
import com.hify.provider.entity.ProviderEntity;

import java.util.Map;

/**
 * Agent 调用上下文聚合 —— 包含 Agent 配置、模型 Provider、适配器、鉴权信息。
 */
public record AgentContext(
        AgentResponse agent,
        String modelId,
        ProviderEntity provider,
        ProviderAdapter adapter,
        Map<String, Object> authConfig
) {}
