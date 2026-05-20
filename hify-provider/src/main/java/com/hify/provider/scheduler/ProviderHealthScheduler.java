package com.hify.provider.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hify.provider.constant.ProviderConstant;
import com.hify.provider.dto.ConnectionTestResult;
import com.hify.provider.entity.ProviderEntity;
import com.hify.provider.entity.ProviderHealthEntity;
import com.hify.provider.mapper.ProviderHealthMapper;
import com.hify.provider.mapper.ProviderMapper;
import com.hify.provider.service.ProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

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
}
