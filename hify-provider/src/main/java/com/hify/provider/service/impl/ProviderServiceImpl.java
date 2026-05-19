package com.hify.provider.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hify.common.exception.BizException;
import com.hify.common.http.LlmApiException;
import com.hify.common.http.LlmHttpClient;
import com.hify.common.result.PageResult;
import com.hify.common.util.PageHelper;
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

    private static final int TEST_TIMEOUT_MS = 10000;

    private final ProviderMapper providerMapper;
    private final ModelConfigMapper modelConfigMapper;
    private final ProviderHealthMapper providerHealthMapper;
    private final LlmHttpClient llmHttpClient;
    private final ObjectMapper objectMapper;

    @Override
    public Long create(ProviderRequest req) {
        ProviderEntity entity = new ProviderEntity();
        BeanUtils.copyProperties(req, entity);
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
        if (entity.getAuthConfig() == null) {
            entity.setAuthConfig(new HashMap<>());
        }
        providerMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void update(Long id, ProviderRequest req) {
        ProviderEntity entity = providerMapper.selectById(id);
        if (entity == null) {
            throw BizException.notFound("提供商不存在");
        }
        Optional.ofNullable(req.getName()).ifPresent(entity::setName);
        Optional.ofNullable(req.getCode()).ifPresent(entity::setCode);
        Optional.ofNullable(req.getType()).ifPresent(entity::setType);
        Optional.ofNullable(req.getBaseUrl()).ifPresent(entity::setBaseUrl);
        Optional.ofNullable(req.getAuthConfig()).ifPresent(entity::setAuthConfig);
        Optional.ofNullable(req.getTimeoutMs()).ifPresent(entity::setTimeoutMs);
        Optional.ofNullable(req.getMaxRetries()).ifPresent(entity::setMaxRetries);
        Optional.ofNullable(req.getRetryIntervalMs()).ifPresent(entity::setRetryIntervalMs);
        Optional.ofNullable(req.getStatus()).ifPresent(entity::setStatus);
        Optional.ofNullable(req.getSortOrder()).ifPresent(entity::setSortOrder);
        Optional.ofNullable(req.getExtraConfig()).ifPresent(entity::setExtraConfig);
        providerMapper.updateById(entity);
    }

    @Override
    public void delete(Long id) {
        providerMapper.deleteById(id);
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
        ProviderEntity entity = providerMapper.selectById(id);
        if (entity == null) {
            throw BizException.notFound("提供商不存在");
        }

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
    @SuppressWarnings("unchecked")
    public ConnectionTestResult testConnection(Long providerId) {
        ProviderEntity entity = providerMapper.selectById(providerId);
        if (entity == null) {
            throw BizException.notFound("提供商不存在");
        }
        Map<String, Object> authConfig = entity.getAuthConfig() instanceof Map
                ? (Map<String, Object>) entity.getAuthConfig()
                : null;
        return testConnection(entity.getBaseUrl(), entity.getType(), authConfig);
    }

    @Override
    public ConnectionTestResult testConnection(String baseUrl, String type, Map<String, Object> authConfig) {
        long start = System.currentTimeMillis();
        ConnectionTestResult result = new ConnectionTestResult();

        String url = buildUrl(baseUrl, type);
        Map<String, String> headers = buildHeaders(type, authConfig);

        try {
            String responseBody = llmHttpClient.get(url, headers, TEST_TIMEOUT_MS);
            long latency = System.currentTimeMillis() - start;

            int modelCount = extractModelCount(responseBody, type);

            result.setSuccess(true);
            result.setLatencyMs(latency);
            result.setModelCount(modelCount);
            log.info("Provider connection test success: type={}, latency={}ms, models={}", type, latency, modelCount);
        } catch (LlmApiException e) {
            long latency = System.currentTimeMillis() - start;
            result.setSuccess(false);
            result.setLatencyMs(latency);
            result.setErrorMessage(e.getMessage());
            log.warn("Provider connection test failed: type={}, error={}", type, e.getMessage());
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            result.setSuccess(false);
            result.setLatencyMs(latency);
            result.setErrorMessage(e.getMessage());
            log.error("Provider connection test exception: type={}", type, e);
        }

        return result;
    }

    private ProviderResponse convertToResponse(ProviderEntity entity) {
        ProviderResponse resp = new ProviderResponse();
        BeanUtils.copyProperties(entity, resp);
        return resp;
    }

    private String buildUrl(String baseUrl, String type) {
        String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        if (ProviderConstant.TYPE_OLLAMA.equalsIgnoreCase(type)) {
            return normalized + "/api/tags";
        }
        return normalized + "/v1/models";
    }

    private Map<String, String> buildHeaders(String type, Map<String, Object> authConfig) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");

        if (ProviderConstant.TYPE_OPENAI.equalsIgnoreCase(type) || ProviderConstant.TYPE_OPENAI_COMPATIBLE.equalsIgnoreCase(type)) {
            String apiKey = extractApiKey(authConfig);
            if (apiKey != null && !apiKey.isEmpty()) {
                headers.put("Authorization", "Bearer " + apiKey);
            }
        } else if (ProviderConstant.TYPE_ANTHROPIC.equalsIgnoreCase(type)) {
            String apiKey = extractApiKey(authConfig);
            if (apiKey != null && !apiKey.isEmpty()) {
                headers.put("x-api-key", apiKey);
            }
            headers.put("anthropic-version", "2023-06-01");
        }

        return headers;
    }

    private String extractApiKey(Map<String, Object> authConfig) {
        if (authConfig == null) {
            return null;
        }
        Object apiKey = authConfig.get("apiKey");
        return apiKey != null ? apiKey.toString() : null;
    }

    private int extractModelCount(String responseBody, String type) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            if (ProviderConstant.TYPE_OLLAMA.equalsIgnoreCase(type)) {
                JsonNode models = root.get("models");
                return models != null && models.isArray() ? models.size() : 0;
            }
            JsonNode data = root.get("data");
            return data != null && data.isArray() ? data.size() : 0;
        } catch (Exception e) {
            log.warn("Failed to parse model count from response: {}", e.getMessage());
            return 0;
        }
    }
}
