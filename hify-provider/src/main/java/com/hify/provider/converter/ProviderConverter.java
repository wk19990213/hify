package com.hify.provider.converter;

import com.hify.provider.dto.ProviderRequest;
import com.hify.provider.dto.ProviderResponse;
import com.hify.provider.entity.ProviderEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * Provider Entity / Request / Response 映射器（MapStruct 替代 BeanUtils.copyProperties）
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProviderConverter {

    ProviderConverter INSTANCE = Mappers.getMapper(ProviderConverter.class);

    /** authConfig 类型不同（Map vs String），由调用方自行加密处理 */
    @Mapping(target = "authConfig", ignore = true)
    ProviderEntity toEntity(ProviderRequest req);

    /** authConfig/extraConfig 类型不同，由调用方自行处理 */
    @Mapping(target = "authConfig", ignore = true)
    @Mapping(target = "extraConfig", ignore = true)
    ProviderResponse toResponse(ProviderEntity entity);
}
