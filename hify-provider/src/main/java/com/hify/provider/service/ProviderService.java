package com.hify.provider.service;

import com.hify.common.result.PageResult;
import com.hify.provider.dto.ConnectionTestResult;
import com.hify.provider.dto.ProviderCreateReq;
import com.hify.provider.dto.ProviderDetailResp;
import com.hify.provider.dto.ProviderResp;
import com.hify.provider.dto.ProviderUpdateReq;

import java.util.Map;

/**
 * 模型提供商服务接口
 */
public interface ProviderService {

    /**
     * 创建提供商
     */
    Long create(ProviderCreateReq req);

    /**
     * 更新提供商
     */
    void update(ProviderUpdateReq req);

    /**
     * 删除提供商（逻辑删除）
     */
    void delete(Long id);

    /**
     * 根据 ID 查询
     */
    ProviderResp getById(Long id);

    /**
     * 查询详情（含模型配置和健康状态）
     */
    ProviderDetailResp getDetail(Long id);

    /**
     * 分页列表
     */
    PageResult<ProviderResp> list(Integer page, Integer size);

    /**
     * 测试提供商连通性（通过配置参数）
     */
    ConnectionTestResult testConnection(String baseUrl, String type, Map<String, Object> authConfig);

    /**
     * 测试提供商连通性（通过 ID）
     */
    ConnectionTestResult testConnection(Long providerId);
}
