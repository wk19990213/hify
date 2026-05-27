package com.hify.provider.service;

import com.hify.provider.dto.ConnectionTestResult;
import com.hify.provider.entity.ProviderHealthEntity;
import com.hify.provider.mapper.ProviderHealthMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Provider 健康状态统一更新服务。
 * 消除 ProviderServiceImpl / ProviderHealthScheduler 中的重复健康更新逻辑。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderHealthService {

    private final ProviderHealthMapper providerHealthMapper;
    private static final int FAIL_THRESHOLD = 3;

    public ProviderHealthEntity updateHealthRecord(Long providerId, ConnectionTestResult result) {
        ProviderHealthEntity health = providerHealthMapper.selectById(providerId);
        boolean isNew = health == null;
        if (isNew) {
            health = new ProviderHealthEntity();
            health.setProviderId(providerId);
        }
        applyResult(health, result);

        if (isNew) {
            try {
                providerHealthMapper.insert(health);
            } catch (DuplicateKeyException e) {
                // TOCTOU: 另一线程已并发插入，回退到更新
                health = providerHealthMapper.selectById(providerId);
                if (health == null) {
                    providerHealthMapper.insert(health);
                } else {
                    applyResult(health, result);
                    providerHealthMapper.updateById(health);
                }
                return health;
            }
        } else {
            providerHealthMapper.updateById(health);
        }
        return health;
    }

    private void applyResult(ProviderHealthEntity health, ConnectionTestResult result) {
        health.setLastCheckTime(LocalDateTime.now());
        if (result.isSuccess()) {
            health.setStatus("HEALTHY");
            health.setConsecutiveFailures(0);
            health.setAvgLatencyMs((int) result.getLatencyMs());
            health.setLastSuccessTime(LocalDateTime.now());
            health.setLastErrorTime(null);
            health.setLastErrorMsg(null);
        } else {
            int fails = health.getConsecutiveFailures() != null
                    ? health.getConsecutiveFailures() + 1 : 1;
            health.setConsecutiveFailures(fails);
            health.setLastErrorTime(LocalDateTime.now());
            health.setLastErrorMsg(result.getErrorMessage());
            if (fails >= FAIL_THRESHOLD) {
                health.setStatus("DOWN");
            }
        }
    }
}
