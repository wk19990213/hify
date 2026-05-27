package com.hify.agent.converter;

import com.hify.agent.dto.AgentRequest;
import com.hify.agent.dto.AgentResponse;
import com.hify.agent.entity.AgentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * Agent Entity / Request / Response 映射器（MapStruct 替代 BeanUtils.copyProperties）
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AgentConverter {

    AgentConverter INSTANCE = Mappers.getMapper(AgentConverter.class);

    AgentEntity toEntity(AgentRequest req);

    AgentResponse toResponse(AgentEntity entity);
}
