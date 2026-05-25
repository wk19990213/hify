package com.hify.provider.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hify.provider.adapter.ProviderAdapter;
import com.hify.provider.adapter.ProviderAdapterFactory;
import com.hify.provider.constant.ProviderConstant;
import com.hify.provider.dto.ConnectionTestResult;
import com.hify.provider.entity.ModelConfigEntity;
import com.hify.provider.entity.ProviderEntity;
import com.hify.provider.entity.ProviderHealthEntity;
import com.hify.provider.entity.ProviderModelEntity;
import com.hify.provider.mapper.ModelConfigMapper;
import com.hify.provider.mapper.ProviderHealthMapper;
import com.hify.provider.mapper.ProviderMapper;
import com.hify.provider.mapper.ProviderModelMapper;
import com.hify.provider.service.ProviderService;
import com.hify.common.crypto.AesEncryptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 供应商健康检查定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProviderHealthScheduler {

    private static final int FAIL_THRESHOLD = 3;

    private final ProviderMapper providerMapper;
    private final ProviderHealthMapper providerHealthMapper;
    private final ProviderService providerService;
    private final ProviderAdapterFactory adapterFactory;
    private final ModelConfigMapper modelConfigMapper;
    private final ProviderModelMapper providerModelMapper;
    private final ObjectMapper objectMapper;

    @Async("asyncExecutor")
    @Scheduled(fixedRate = 60000)
    public void checkAllProviders() {
        log.debug("Starting provider health check...");
        List<ProviderEntity> enabledProviders = providerMapper.selectList(
                new LambdaQueryWrapper<ProviderEntity>()
                        .eq(ProviderEntity::getStatus, ProviderConstant.STATUS_ENABLED)
                        .eq(ProviderEntity::getDeleted, 0)
        );

        for (ProviderEntity provider : enabledProviders) {
            try {
                ConnectionTestResult result = providerService.testConnection(provider.getId());
                updateHealth(provider.getId(), result);
            } catch (Exception e) {
                log.error("Health check failed for provider {}: {}", provider.getId(), e.getMessage());
            }
        }
    }

    private void updateHealth(Long providerId, ConnectionTestResult result) {
        ProviderHealthEntity health = providerHealthMapper.selectById(providerId);
        boolean isNew = health == null;
        if (isNew) {
            health = new ProviderHealthEntity();
            health.setProviderId(providerId);
        }

        health.setLastCheckTime(LocalDateTime.now());

        if (result.isSuccess()) {
            health.setStatus("HEALTHY");
            health.setConsecutiveFailures(0);
            health.setAvgLatencyMs((int) result.getLatencyMs());
            health.setLastSuccessTime(LocalDateTime.now());
            health.setLastErrorTime(null);
            health.setLastErrorMsg(null);
            log.info("Provider {} health: UP, latency={}ms", providerId, result.getLatencyMs());
        } else {
            int fails = health.getConsecutiveFailures() != null ? health.getConsecutiveFailures() + 1 : 1;
            health.setConsecutiveFailures(fails);
            health.setLastErrorTime(LocalDateTime.now());
            health.setLastErrorMsg(result.getErrorMessage());

            if (fails >= FAIL_THRESHOLD) {
                health.setStatus("DOWN");
                log.warn("Provider {} health: DOWN (consecutive failures: {})", providerId, fails);
            } else {
                log.info("Provider {} health check failed (consecutive failures: {})", providerId, fails);
            }
        }

        if (isNew) {
            providerHealthMapper.insert(health);
        } else {
            providerHealthMapper.updateById(health);
        }
    }

    /** 每 5 分钟同步一次所有 Provider 的对话模型列表（按 model_id 去重，维护 provider_count） */
    @Async("asyncExecutor")
    @Scheduled(fixedRate = 300000)
    public void syncAllModels() {
        List<ProviderEntity> enabledProviders = providerMapper.selectList(
                new LambdaQueryWrapper<ProviderEntity>()
                        .eq(ProviderEntity::getStatus, ProviderConstant.STATUS_ENABLED)
                        .eq(ProviderEntity::getDeleted, 0));
        for (ProviderEntity p : enabledProviders) {
            try {
                String json = AesEncryptor.decrypt(p.getAuthConfig());
                @SuppressWarnings("unchecked")
                Map<String, Object> auth = json != null && !json.isEmpty()
                        ? objectMapper.readValue(json, Map.class) : null;
                ProviderAdapter adapter = adapterFactory.getAdapter(p.getType());
                List<String> modelIds = adapter.listModelIds(p.getBaseUrl(), auth);
                for (String modelId : modelIds) {
                    String lower = modelId.toLowerCase();
                    if (lower.contains("embed") || lower.contains("rerank") || lower.contains("image")
                            || lower.contains("ocr") || lower.contains("tts") || lower.contains("asr")
                            || lower.contains("caption") || lower.contains("lora")) continue;

                    // 1. UPSERT provider_model
                    var existPm = providerModelMapper.selectCount(new LambdaQueryWrapper<ProviderModelEntity>()
                            .eq(ProviderModelEntity::getProviderId, p.getId())
                            .eq(ProviderModelEntity::getModelId, modelId));
                    if (existPm == 0) {
                        ProviderModelEntity pm = new ProviderModelEntity();
                        pm.setProviderId(p.getId());
                        pm.setModelId(modelId);
                        providerModelMapper.insert(pm);
                    }

                    // 2. 按 model_id 查找 model_config（全局唯一）
                    ModelConfigEntity mc = modelConfigMapper.selectOne(
                            new LambdaQueryWrapper<ModelConfigEntity>()
                                    .eq(ModelConfigEntity::getModelId, modelId)
                                    .eq(ModelConfigEntity::getDeleted, 0));

                    // 3. 重新计算 provider_count
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
                        mc.setProviderId(p.getId());
                        mc.setModelId(modelId);
                        mc.setName(modelId);
                        mc.setCode(modelId.replace("/", "-").replace(":", "-"));
                        mc.setStatus(1);
                        mc.setSortOrder(0);
                        mc.setProviderCount(1);
                        modelConfigMapper.insert(mc);
                    }
                }
            } catch (Exception e) {
                log.warn("Model sync failed for provider {}: {}", p.getName(), e.getMessage());
            }
        }
    }
}
