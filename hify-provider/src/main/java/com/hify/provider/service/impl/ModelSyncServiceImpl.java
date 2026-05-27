package com.hify.provider.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hify.provider.adapter.ProviderAdapter;
import com.hify.provider.adapter.ProviderAdapterFactory;
import com.hify.provider.entity.ModelConfigEntity;
import com.hify.provider.entity.ProviderEntity;
import com.hify.provider.entity.ProviderModelEntity;
import com.hify.provider.mapper.ModelConfigMapper;
import com.hify.provider.mapper.ProviderModelMapper;
import com.hify.provider.service.ModelSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 模型同步服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelSyncServiceImpl implements ModelSyncService {

    private final ProviderAdapterFactory adapterFactory;
    private final ProviderModelMapper providerModelMapper;
    private final ModelConfigMapper modelConfigMapper;

    /** 调用 Provider API 获取模型列表，按 model_id 去重写入 model_config，记录到 provider_model */
    @Override
    public void syncModels(ProviderEntity provider, Map<String, Object> authConfig) {
        try {
            ProviderAdapter adapter = adapterFactory.getAdapter(provider.getType());
            List<String> modelIds = adapter.listModelIds(provider.getBaseUrl(), authConfig);
            for (String modelId : modelIds) {
                if (shouldSkipModel(modelId)) continue;
                syncSingleModel(provider, modelId);
            }
            log.info("Synced {} models for provider {}", modelIds.size(), provider.getName());
        } catch (Exception e) {
            log.warn("Failed to sync models for provider {}: {}", provider.getName(), e.getMessage());
        }
    }

    /** 同步单个模型：UPSERT provider_model + UPSERT model_config */
    @Override
    public void syncSingleModel(ProviderEntity provider, String modelId) {
        // 1. UPSERT provider_model
        var existPm = providerModelMapper.selectCount(new LambdaQueryWrapper<ProviderModelEntity>()
                .eq(ProviderModelEntity::getProviderId, provider.getId())
                .eq(ProviderModelEntity::getModelId, modelId));
        if (existPm == 0) {
            ProviderModelEntity pm = new ProviderModelEntity();
            pm.setProviderId(provider.getId());
            pm.setModelId(modelId);
            providerModelMapper.insert(pm);
        }

        // 2. 按 model_id 查找 model_config（全局唯一）
        ModelConfigEntity mc = modelConfigMapper.selectOne(
                new LambdaQueryWrapper<ModelConfigEntity>()
                        .eq(ModelConfigEntity::getModelId, modelId)
                        .eq(ModelConfigEntity::getDeleted, 0));

        // 3. 从 provider_model 重新计算 provider_count
        long count = providerModelMapper.selectCount(
                new LambdaQueryWrapper<ProviderModelEntity>()
                        .eq(ProviderModelEntity::getModelId, modelId));

        if (mc != null) {
            mc.setProviderCount((int) count);
            if (mc.getStatus() == 0 && count > 0) {
                mc.setStatus(1);
            }
            modelConfigMapper.updateById(mc);
        } else {
            mc = new ModelConfigEntity();
            mc.setProviderId(provider.getId());
            mc.setModelId(modelId);
            mc.setName(modelId);
            mc.setCode(modelId.replace("/", "-").replace(":", "-"));
            mc.setStatus(1);
            mc.setSortOrder(0);
            mc.setProviderCount(1);
            modelConfigMapper.insert(mc);
        }
    }

    /** 过滤非对话模型（Embedding/Rerank/Image/TTS 等） */
    private boolean shouldSkipModel(String modelId) {
        String lower = modelId.toLowerCase();
        return lower.contains("embed") || lower.contains("rerank") || lower.contains("image")
                || lower.contains("ocr") || lower.contains("tts") || lower.contains("asr")
                || lower.contains("caption") || lower.contains("lora");
    }
}
