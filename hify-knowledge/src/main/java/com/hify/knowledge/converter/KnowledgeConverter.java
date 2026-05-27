package com.hify.knowledge.converter;

import com.hify.knowledge.dto.DocumentResp;
import com.hify.knowledge.dto.KnowledgeBaseResp;
import com.hify.knowledge.entity.DocumentEntity;
import com.hify.knowledge.entity.KnowledgeBaseEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * Knowledge Entity / Response 映射器（MapStruct 替代 BeanUtils.copyProperties）
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface KnowledgeConverter {

    KnowledgeConverter INSTANCE = Mappers.getMapper(KnowledgeConverter.class);

    KnowledgeBaseResp toKBResp(KnowledgeBaseEntity entity);

    DocumentResp toDocResp(DocumentEntity entity);
}
