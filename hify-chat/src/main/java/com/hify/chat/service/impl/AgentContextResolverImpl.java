package com.hify.chat.service.impl;

import com.hify.agent.dto.AgentResponse;
import com.hify.agent.service.AgentService;
import com.hify.chat.dto.AgentContext;
import com.hify.chat.entity.ChatSessionEntity;
import com.hify.chat.service.AgentContextResolver;
import com.hify.common.exception.BizException;
import com.hify.provider.adapter.ProviderAdapter;
import com.hify.provider.adapter.ProviderAdapterFactory;
import com.hify.provider.entity.ModelConfigEntity;
import com.hify.provider.entity.ProviderEntity;
import com.hify.provider.mapper.ModelConfigMapper;
import com.hify.provider.service.ProviderDiscoveryService;
import com.hify.provider.util.AuthConfigHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgentContextResolverImpl implements AgentContextResolver {

    private final AgentService agentService;
    private final ModelConfigMapper modelConfigMapper;
    private final ProviderDiscoveryService providerDiscoveryService;
    private final ProviderAdapterFactory adapterFactory;

    @Override
    public AgentContext resolveContext(ChatSessionEntity session) {
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

        ProviderEntity provider = findAvailableProvider(modelConfig);

        ProviderAdapter adapter = adapterFactory.getAdapter(provider.getType());

        Map<String, Object> authConfig = AuthConfigHelper.decryptAuthConfig(provider.getAuthConfig());
        if (authConfig.isEmpty()) authConfig = null;

        return new AgentContext(agent, modelConfig.getModelId(), provider, adapter, authConfig);
    }

    @Override
    public ProviderEntity findAvailableProvider(ModelConfigEntity modelConfig) {
        ProviderEntity provider = providerDiscoveryService.findAvailableProviderByModelId(modelConfig.getModelId());
        if (provider == null) {
            throw BizException.notFound("模型的所有提供商均不可用");
        }
        return provider;
    }
}
