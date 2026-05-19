package com.hify.provider.dto;

import com.hify.provider.entity.ModelConfigEntity;
import com.hify.provider.entity.ProviderHealthEntity;
import lombok.Data;

import java.util.List;

/**
 * 提供商详情响应（含模型配置和健康状态）
 */
@Data
public class ProviderDetailResp {

    private ProviderResp provider;

    private List<ModelConfigEntity> modelConfigs;

    private ProviderHealthEntity health;
}
