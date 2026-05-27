package com.hify.workflow.converter;

import com.hify.workflow.dto.NodeExecutionResp;
import com.hify.workflow.dto.WorkflowInstanceResp;
import com.hify.workflow.dto.WorkflowResp;
import com.hify.workflow.entity.NodeExecutionEntity;
import com.hify.workflow.entity.WorkflowEntity;
import com.hify.workflow.entity.WorkflowInstanceEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * Workflow Entity / Response 映射器（MapStruct 替代 BeanUtils.copyProperties）
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface WorkflowConverter {

    WorkflowConverter INSTANCE = Mappers.getMapper(WorkflowConverter.class);

    WorkflowResp toResponse(WorkflowEntity entity);

    WorkflowInstanceResp toInstanceResp(WorkflowInstanceEntity entity);

    NodeExecutionResp toNodeExecutionResp(NodeExecutionEntity entity);
}
