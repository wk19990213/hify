package com.hify.agent.mapper;

import com.hify.agent.dto.AgentResponse;
import com.hify.agent.entity.AgentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * Agent Entity ↔ Response 映射器（MapStruct 替代 BeanUtils.copyProperties）
 */
@Mapper
public interface AgentConvertMapper {

    AgentConvertMapper INSTANCE = Mappers.getMapper(AgentConvertMapper.class);

    AgentResponse toResponse(AgentEntity entity);
}
