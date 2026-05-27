package com.hify.chat.service;

import com.hify.chat.dto.AgentContext;
import com.hify.chat.entity.ChatSessionEntity;
import com.hify.provider.entity.ModelConfigEntity;
import com.hify.provider.entity.ProviderEntity;

/**
 * Agent 调用上下文解析器 —— 从会话中解析出 Agent 配置、模型 Provider、适配器、鉴权信息。
 */
public interface AgentContextResolver {

    /** Agent → ModelConfig → model_id → provider_model → Provider → Adapter */
    AgentContext resolveContext(ChatSessionEntity session);

    /** 查找可用的 Provider，优先使用 model_config 记录的 provider_id */
    ProviderEntity findAvailableProvider(ModelConfigEntity modelConfig);
}
