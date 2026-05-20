package com.hify.provider.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hify.common.crypto.AesEncryptor;
import com.hify.common.exception.BizException;
import com.hify.common.result.PageResult;
import com.hify.common.util.PageHelper;
import com.hify.provider.adapter.ProviderAdapter;
import com.hify.provider.adapter.ProviderAdapterFactory;
import com.hify.provider.constant.ProviderConstant;
import com.hify.provider.dto.ConnectionTestResult;
import com.hify.provider.dto.ProviderRequest;
import com.hify.provider.dto.ProviderResponse;
import com.hify.provider.entity.ModelConfigEntity;
import com.hify.provider.entity.ProviderEntity;
import com.hify.provider.entity.ProviderHealthEntity;
import com.hify.provider.mapper.ModelConfigMapper;
import com.hify.provider.mapper.ProviderHealthMapper;
import com.hify.provider.mapper.ProviderMapper;
import com.hify.provider.service.ProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 模型提供商服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderServiceImpl implements ProviderService {

    private final ProviderMapper providerMapper;
    private final ModelConfigMapper modelConfigMapper;
    private final ProviderHealthMapper providerHealthMapper;
    private final ObjectMapper objectMapper;
    private final CacheManager cacheManager;
    private final ProviderAdapterFactory adapterFactory;

    @Override
    public Long create(ProviderRequest req) {
        ProviderEntity entity = new ProviderEntity();
        BeanUtils.copyProperties(req, entity, "authConfig");
        if (entity.getCode() == null || entity.getCode().isBlank()) {
            entity.setCode(req.getType().toLowerCase() + "-" + java.util.UUID.randomUUID().toString().substring(0, 8));
        }
        if (entity.getTimeoutMs() == null) {
            entity.setTimeoutMs(30000);
        }
        if (entity.getMaxRetries() == null) {
            entity.setMaxRetries(3);
        }
        if (entity.getRetryIntervalMs() == null) {
            entity.setRetryIntervalMs(1000);
        }
        if (entity.getStatus() == null) {
            entity.setStatus(ProviderConstant.STATUS_ENABLED);
        }
        if (entity.getSortOrder() == null) {
            entity.setSortOrder(0);
        }
        entity.setAuthConfig(encryptAuthConfig(req.getAuthConfig()));
        providerMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void update(Long id, ProviderRequest req) {
        ProviderEntity entity = getProviderById(id);
        Optional.ofNullable(req.getName()).ifPresent(entity::setName);
        Optional.ofNullable(req.getCode()).ifPresent(entity::setCode);
        Optional.ofNullable(req.getType()).ifPresent(entity::setType);
        Optional.ofNullable(req.getBaseUrl()).ifPresent(entity::setBaseUrl);
        Optional.ofNullable(req.getTimeoutMs()).ifPresent(entity::setTimeoutMs);
        Optional.ofNullable(req.getMaxRetries()).ifPresent(entity::setMaxRetries);
        Optional.ofNullable(req.getRetryIntervalMs()).ifPresent(entity::setRetryIntervalMs);
        Optional.ofNullable(req.getStatus()).ifPresent(entity::setStatus);
        Optional.ofNullable(req.getSortOrder()).ifPresent(entity::setSortOrder);
        Optional.ofNullable(req.getExtraConfig()).ifPresent(entity::setExtraConfig);
        if (req.getAuthConfig() != null) {
            entity.setAuthConfig(encryptAuthConfig(req.getAuthConfig()));
        }
        providerMapper.updateById(entity);
        evictProviderCache(id);
    }

    @Override
    public void delete(Long id) {
        getProviderById(id);
        providerMapper.deleteById(id);
        evictProviderCache(id);
    }

    @Override
    public PageResult<ProviderResponse> list(Integer page, Integer size) {
        var pageParam = PageHelper.<ProviderEntity>toPage(page, size);
        var wrapper = new LambdaQueryWrapper<ProviderEntity>()
                .orderByAsc(ProviderEntity::getSortOrder)
                .orderByDesc(ProviderEntity::getCreatedAt);
        var pageResult = providerMapper.selectPage(pageParam, wrapper);

        List<ProviderResponse> responses = pageResult.getRecords().stream()
                .map(this::convertToResponse)
                .toList();

        // 批量填充：健康状态 + 模型数
        List<Long> providerIds = responses.stream().map(ProviderResponse::getId).toList();
        if (!providerIds.isEmpty()) {
            var healthMap = providerHealthMapper.selectList(
                            new LambdaQueryWrapper<ProviderHealthEntity>()
                                    .in(ProviderHealthEntity::getProviderId, providerIds))
                    .stream()
                    .collect(java.util.stream.Collectors.toMap(
                            ProviderHealthEntity::getProviderId, h -> h, (a, b) -> a));

            var modelCountMap = modelConfigMapper.selectList(
                            new LambdaQueryWrapper<ModelConfigEntity>()
                                    .in(ModelConfigEntity::getProviderId, providerIds)
                                    .eq(ModelConfigEntity::getStatus, 1))
                    .stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            ModelConfigEntity::getProviderId,
                            java.util.stream.Collectors.counting()));

            for (ProviderResponse resp : responses) {
                resp.setHealth(healthMap.get(resp.getId()));
                resp.setModelCount(modelCountMap.getOrDefault(resp.getId(), 0L).intValue());
            }
        }

        return PageResult.ok(
                responses,
                pageResult.getTotal(),
                pageResult.getCurrent(),
                pageResult.getSize()
        );
    }

    @Override
    public ProviderResponse getDetail(Long id) {
        ProviderEntity entity = getProviderById(id);

        List<ModelConfigEntity> modelConfigs = modelConfigMapper.selectList(
                new LambdaQueryWrapper<ModelConfigEntity>()
                        .eq(ModelConfigEntity::getProviderId, id)
                        .orderByAsc(ModelConfigEntity::getSortOrder)
        );

        ProviderHealthEntity health = providerHealthMapper.selectOne(
                new LambdaQueryWrapper<ProviderHealthEntity>()
                        .eq(ProviderHealthEntity::getProviderId, id)
        );

        ProviderResponse resp = convertToResponse(entity);
        resp.setModelConfigs(modelConfigs);
        resp.setHealth(health);
        return resp;
    }

    @Override
    public ConnectionTestResult testConnection(Long providerId) {
        ProviderEntity entity = getProviderById(providerId);
        Map<String, Object> authConfig = decryptAuthConfig(entity.getAuthConfig());
        // 策略模式：根据 type 获取对应 Adapter
        ProviderAdapter adapter = adapterFactory.getAdapter(entity.getType());
        ConnectionTestResult result = adapter.testConnection(entity, authConfig);
        updateProviderHealth(providerId, result);
        return result;
    }

    @Override
    public ConnectionTestResult testConnection(String baseUrl, String type, Map<String, Object> authConfig) {
        // 构造临时 entity 供 Adapter 使用
        ProviderEntity temp = new ProviderEntity();
        temp.setBaseUrl(baseUrl);
        temp.setType(type);
        ProviderAdapter adapter = adapterFactory.getAdapter(type);
        return adapter.testConnection(temp, authConfig);
    }

    @SuppressWarnings("unchecked")
    private void updateProviderHealth(Long providerId, ConnectionTestResult result) {
        ProviderHealthEntity health = providerHealthMapper.selectById(providerId);
        boolean isNew = health == null;
        if (isNew) {
            health = new ProviderHealthEntity();
            health.setProviderId(providerId);
        }
        health.setLastCheckTime(java.time.LocalDateTime.now());
        if (result.isSuccess()) {
            health.setStatus("HEALTHY");
            health.setConsecutiveFailures(0);
            health.setAvgLatencyMs((int) result.getLatencyMs());
            health.setLastSuccessTime(java.time.LocalDateTime.now());
            health.setLastErrorTime(null);
            health.setLastErrorMsg(null);
        } else {
            int fails = health.getConsecutiveFailures() != null ? health.getConsecutiveFailures() + 1 : 1;
            health.setConsecutiveFailures(fails);
            health.setLastErrorTime(java.time.LocalDateTime.now());
            health.setLastErrorMsg(result.getErrorMessage());
            if (fails >= 3) {
                health.setStatus("DOWN");
            }
        }
        if (isNew) {
            providerHealthMapper.insert(health);
        } else {
            providerHealthMapper.updateById(health);
        }
    }

    private ProviderEntity getProviderById(Long id) {
        Cache cache = cacheManager.getCache("provider-cache");
        if (cache != null) {
            try {
                ProviderEntity cached = cache.get(id, ProviderEntity.class);
                if (cached != null) {
                    return cached;
                }
            } catch (Exception e) {
                log.warn("Cache read failed for provider {}, falling through to DB", id, e);
            }
        }
        ProviderEntity entity = providerMapper.selectById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw BizException.notFound("提供商不存在");
        }
        if (cache != null) {
            try {
                cache.put(id, entity);
            } catch (Exception e) {
                log.warn("Cache write failed for provider {}", id, e);
            }
        }
        return entity;
    }

    private String encryptAuthConfig(Map<String, Object> authConfig) {
        if (authConfig == null || authConfig.isEmpty()) {
            return "{}";
        }
        try {
            String json = objectMapper.writeValueAsString(authConfig);
            return AesEncryptor.encrypt(json);
        } catch (Exception e) {
            log.error("Failed to encrypt authConfig", e);
            return "{}";
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> decryptAuthConfig(String encrypted) {
        if (encrypted == null || encrypted.isEmpty()) {
            return null;
        }
        try {
            String json = AesEncryptor.decrypt(encrypted);
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            log.error("Failed to decrypt authConfig", e);
            return null;
        }
    }

    private void evictProviderCache(Long id) {
        Cache cache = cacheManager.getCache("provider-cache");
        if (cache != null) {
            try {
                cache.evict(id);
            } catch (Exception e) {
                log.warn("Cache evict failed for provider {}", id, e);
            }
        }
    }

    private ProviderResponse convertToResponse(ProviderEntity entity) {
        ProviderResponse resp = new ProviderResponse();
        BeanUtils.copyProperties(entity, resp, "authConfig");
        // 解密并脱敏 authConfig
        Map<String, Object> authMap = decryptAuthConfig(entity.getAuthConfig());
        if (authMap != null) {
            Map<String, Object> masked = new HashMap<>(authMap);
            if (masked.containsKey("apiKey")) {
                String key = (String) masked.get("apiKey");
                if (key != null && key.length() > 8) {
                    masked.put("apiKey", key.substring(0, 4) + "****" + key.substring(key.length() - 4));
                }
            }
            resp.setAuthConfig(masked);
        }
        return resp;
    }
}
