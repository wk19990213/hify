package com.hify.provider.service;

import com.hify.provider.entity.ProviderEntity;

import java.util.Map;

/**
 * 模型同步服务 — 从 Provider API 拉取模型列表并同步到本地数据库。
 */
public interface ModelSyncService {

    /**
     * 调用 Provider API 获取模型列表，按 model_id 去重写入 model_config，记录到 provider_model
     */
    void syncModels(ProviderEntity provider, Map<String, Object> authConfig);

    /**
     * 同步单个模型：UPSERT provider_model + UPSERT model_config
     */
    void syncSingleModel(ProviderEntity provider, String modelId);
}
