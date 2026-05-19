package com.hify.provider.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hify.provider.entity.ProviderHealthEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 供应商健康状态 Mapper
 */
@Mapper
public interface ProviderHealthMapper extends BaseMapper<ProviderHealthEntity> {
}
