package com.hify.provider.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hify.provider.constant.ProviderConstant;
import com.hify.provider.dto.ConnectionTestResult;
import com.hify.provider.entity.ProviderEntity;
import com.hify.provider.mapper.ProviderMapper;
import com.hify.provider.service.ProviderHealthService;
import com.hify.provider.service.ProviderService;
import com.hify.provider.util.AuthConfigHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 供应商健康检查与模型同步定时任务。
 * 健康更新委托给 ProviderHealthService，模型同步委托给 ProviderService.syncModels。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProviderHealthScheduler {

    private final ProviderMapper providerMapper;
    private final ProviderService providerService;
    private final ProviderHealthService providerHealthService;

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
                providerHealthService.updateHealthRecord(provider.getId(), result);
            } catch (Exception e) {
                log.error("Health check failed for provider {}: {}", provider.getId(), e.getMessage());
            }
        }
    }

    /** 每 5 分钟同步一次所有 Provider 的对话模型列表（委托给 ProviderService，消除重复逻辑） */
    @Async("asyncExecutor")
    @Scheduled(fixedRate = 300000)
    public void syncAllModels() {
        List<ProviderEntity> enabledProviders = providerMapper.selectList(
                new LambdaQueryWrapper<ProviderEntity>()
                        .eq(ProviderEntity::getStatus, ProviderConstant.STATUS_ENABLED)
                        .eq(ProviderEntity::getDeleted, 0));
        for (ProviderEntity p : enabledProviders) {
            try {
                Map<String, Object> auth = AuthConfigHelper.decryptAuthConfig(p.getAuthConfig());
                if (auth.isEmpty()) auth = null;
                providerService.syncModels(p, auth);
            } catch (Exception e) {
                log.warn("Model sync failed for provider {}: {}", p.getName(), e.getMessage());
            }
        }
    }
}
