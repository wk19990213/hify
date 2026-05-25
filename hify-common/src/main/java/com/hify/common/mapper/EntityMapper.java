package com.hify.common.mapper;

import org.mapstruct.MapperConfig;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct 实体映射器基类配置。
 * 各业务模块可创建子接口继承此配置，定义具体的实体→DTO 转换。
 *
 * <p>使用示例：
 * <pre>
 * &#064;Mapper(config = EntityMapper.class)
 * public interface AgentConverter {
 *     AgentResponse toResponse(AgentEntity entity);
 *     List&lt;AgentResponse&gt; toResponseList(List&lt;AgentEntity&gt; entities);
 * }
 * </pre>
 */
@MapperConfig(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface EntityMapper {
}
