package com.hify.mcp.converter;

import com.hify.mcp.dto.McpServerCreateReq;
import com.hify.mcp.dto.McpServerResp;
import com.hify.mcp.entity.McpServerEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * MCP Server Entity / Request / Response 映射器（MapStruct 替代 BeanUtils.copyProperties）
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface McpServerConverter {

    McpServerConverter INSTANCE = Mappers.getMapper(McpServerConverter.class);

    McpServerEntity toEntity(McpServerCreateReq req);

    McpServerResp toResponse(McpServerEntity entity);
}
