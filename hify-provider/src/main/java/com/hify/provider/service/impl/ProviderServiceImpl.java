package com.hify.provider.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hify.common.crypto.AesEncryptor;
import com.hify.common.exception.BizException;
import com.hify.common.result.PageResult;
import com.hify.common.util.UrlSecurityValidator;
import com.hify.common.util.PageHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.hify.provider.adapter.ProviderAdapter;
import com.hify.provider.adapter.ProviderAdapterFactory;
import com.hify.provider.constant.ProviderConstant;
import com.hify.provider.dto.ConnectionTestResult;
import com.hify.provider.dto.ProviderRequest;
import com.hify.provider.dto.ProviderResponse;
import com.hify.provider.entity.ModelConfigEntity;
import com.hify.provider.entity.ProviderEntity;
import com.hify.provider.entity.ProviderHealthEntity;
import com.hify.provider.entity.ProviderModelEntity;
import com.hify.provider.mapper.ModelConfigMapper;
import com.hify.provider.mapper.ProviderHealthMapper;
import com.hify.provider.mapper.ProviderMapper;
import com.hify.provider.mapper.ProviderModelMapper;
import com.hify.provider.service.ModelSyncService;
import com.hify.provider.service.ProviderHealthService;
import com.hify.provider.service.ProviderService;
import com.hify.provider.util.AuthConfigHelper;
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
    private final ProviderHealthService providerHealthService;
    private final ProviderModelMapper providerModelMapper;
    private final ObjectMapper objectMapper;
    private final CacheManager cacheManager;
    private final ProviderAdapterFactory adapterFactory;
    private final ModelSyncService modelSyncService;

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
        // SSRF 防护
        UrlSecurityValidator.validateUrl(entity.getBaseUrl(), "baseUrl");
        // 自动发现模型并同步到 model_config
        modelSyncService.syncModels(entity, req.getAuthConfig());
        return entity.getId();
    }

    @Override
    public void syncModels(ProviderEntity provider, Map<String, Object> authConfig) {
        modelSyncService.syncModels(provider, authConfig);
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

        // 1. 查出该 Provider 提供的所有 model_id
        List<ProviderModelEntity> pmList = providerModelMapper.selectList(
                new LambdaQueryWrapper<ProviderModelEntity>()
                        .eq(ProviderModelEntity::getProviderId, id));
        List<String> affectedModelIds = pmList.stream()
                .map(ProviderModelEntity::getModelId).distinct().toList();

        // 2. 删除 Provider
        providerMapper.deleteById(id);

        // 3. 删除 provider_model 关联
        providerModelMapper.delete(
                new LambdaQueryWrapper<ProviderModelEntity>()
                        .eq(ProviderModelEntity::getProviderId, id));

        recalculateModelProviderCounts(affectedModelIds, id);

        evictProviderCache(id);
    }

    private void recalculateModelProviderCounts(List<String> affectedModelIds, Long deletedProviderId) {
        for (String modelId : affectedModelIds) {
            long remaining = providerModelMapper.selectCount(
                    new LambdaQueryWrapper<ProviderModelEntity>()
                            .eq(ProviderModelEntity::getModelId, modelId));

            ModelConfigEntity mc = modelConfigMapper.selectOne(
                    new LambdaQueryWrapper<ModelConfigEntity>()
                            .eq(ModelConfigEntity::getModelId, modelId)
                            .eq(ModelConfigEntity::getDeleted, 0));

            if (mc != null) {
                mc.setProviderCount((int) remaining);
                if (remaining <= 0) {
                    mc.setStatus(0);
                } else if (mc.getProviderId().equals(deletedProviderId)) {
                    Page<ProviderModelEntity> altPage = providerModelMapper.selectPage(
                            Page.of(1, 1),
                            new LambdaQueryWrapper<ProviderModelEntity>()
                                    .eq(ProviderModelEntity::getModelId, modelId));
                    ProviderModelEntity alt = altPage.getRecords().isEmpty() ? null : altPage.getRecords().get(0);
                    if (alt != null) {
                        mc.setProviderId(alt.getProviderId());
                    }
                }
                modelConfigMapper.updateById(mc);
            }
        }
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

        enrichHealthStatus(responses);
        enrichModelCounts(responses);

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

        ProviderHealthEntity health = providerHealthMapper.selectOne(
                new LambdaQueryWrapper<ProviderHealthEntity>()
                        .eq(ProviderHealthEntity::getProviderId, id)
        );

        // 通过 provider_model 查询该 Provider 的模型，再加载 model_config
        List<String> modelIds = providerModelMapper.selectList(
                        new LambdaQueryWrapper<ProviderModelEntity>()
                                .eq(ProviderModelEntity::getProviderId, id))
                .stream()
                .map(ProviderModelEntity::getModelId)
                .distinct()
                .toList();
        List<ModelConfigEntity> modelConfigs = modelIds.isEmpty() ? List.of()
                : modelConfigMapper.selectList(
                        new LambdaQueryWrapper<ModelConfigEntity>()
                                .in(ModelConfigEntity::getModelId, modelIds)
                                .eq(ModelConfigEntity::getDeleted, 0)
                                .orderByAsc(ModelConfigEntity::getSortOrder));

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
        providerHealthService.updateHealthRecord(providerId, result);
        // 同步模型列表（连通成功了就更新 model_config）
        if (result.isSuccess()) modelSyncService.syncModels(entity, authConfig);
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

    private Map<String, Object> decryptAuthConfig(String encrypted) {
        Map<String, Object> result = AuthConfigHelper.decryptAuthConfig(encrypted);
        return result.isEmpty() ? null : result;
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

    private void enrichHealthStatus(List<ProviderResponse> responses) {
        List<Long> providerIds = responses.stream().map(ProviderResponse::getId).toList();
        if (providerIds.isEmpty()) return;
        var healthMap = providerHealthMapper.selectList(
                        new LambdaQueryWrapper<ProviderHealthEntity>()
                                .in(ProviderHealthEntity::getProviderId, providerIds))
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        ProviderHealthEntity::getProviderId, h -> h, (a, b) -> a));
        for (ProviderResponse resp : responses) {
            resp.setHealth(healthMap.get(resp.getId()));
        }
    }

    private void enrichModelCounts(List<ProviderResponse> responses) {
        for (ProviderResponse resp : responses) {
            long c = providerModelMapper.selectCount(
                    new LambdaQueryWrapper<ProviderModelEntity>()
                            .eq(ProviderModelEntity::getProviderId, resp.getId()));
            resp.setModelCount((int) c);
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
