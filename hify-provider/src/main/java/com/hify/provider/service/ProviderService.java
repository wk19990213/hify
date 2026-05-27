package com.hify.provider.service;

import com.hify.common.result.PageResult;
import com.hify.provider.dto.ConnectionTestResult;
import com.hify.provider.dto.ProviderRequest;
import com.hify.provider.dto.ProviderResponse;
import com.hify.provider.entity.ProviderEntity;

import java.util.Map;

/**
 * 模型提供商服务接口
 */
public interface ProviderService {

    /**
     * 创建提供商
     */
    Long create(ProviderRequest req);

    /**
     * 更新提供商
     */
    void update(Long id, ProviderRequest req);

    /**
     * 删除提供商（逻辑删除）
     */
    void delete(Long id);

    /**
     * 分页列表
     */
    PageResult<ProviderResponse> list(Integer page, Integer size);

    /**
     * 查看详情（含模型配置和健康状态）
     */
    ProviderResponse getDetail(Long id);

    /**
     * 测试提供商连通性（通过配置参数）
     */
    ConnectionTestResult testConnection(String baseUrl, String type, Map<String, Object> authConfig);

    /**
     * 测试提供商连通性（通过 ID）
     */
    ConnectionTestResult testConnection(Long providerId);

    /**
     * 同步 Provider 的模型列表到 model_config 表。
     * ProviderHealthScheduler 定时调用此方法，避免重复同步逻辑。
     */
    void syncModels(ProviderEntity provider, Map<String, Object> authConfig);
}
