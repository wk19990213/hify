package com.hify.provider.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hify.provider.entity.ModelConfigEntity;
import com.hify.provider.entity.ProviderEntity;
import com.hify.provider.entity.ProviderModelEntity;
import com.hify.provider.mapper.ModelConfigMapper;
import com.hify.provider.mapper.ProviderMapper;
import com.hify.provider.mapper.ProviderModelMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Provider 发现服务 — 通过 model_id 查找可用 Provider。
 * 消除 ChatServiceImpl / KnowledgeServiceImpl / LlmNodeExecutor 中的重复查找逻辑。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderDiscoveryService {

    private final ModelConfigMapper modelConfigMapper;
    private final ProviderModelMapper providerModelMapper;
    private final ProviderMapper providerMapper;

    /** 通过 modelConfigId 查找第一个可用 Provider */
    public ProviderEntity findAvailableProvider(Long modelConfigId) {
        ModelConfigEntity modelConfig = modelConfigMapper.selectById(modelConfigId);
        if (modelConfig == null || modelConfig.getDeleted() == 1) return null;
        return findAvailableProviderByModelId(modelConfig.getModelId());
    }

    /** 通过 model_id 查找第一个可用 Provider */
    public ProviderEntity findAvailableProviderByModelId(String modelId) {
        List<ProviderModelEntity> pmList = providerModelMapper.selectList(
                new LambdaQueryWrapper<ProviderModelEntity>()
                        .eq(ProviderModelEntity::getModelId, modelId));
        for (ProviderModelEntity pm : pmList) {
            ProviderEntity p = providerMapper.selectById(pm.getProviderId());
            if (p != null && p.getDeleted() == 0 && p.getStatus() == 1) return p;
        }
        return null;
    }
}
