package com.hify.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hify.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_base")
public class KnowledgeBaseEntity extends BaseEntity {
    private String name;
    private String description;
    private String embeddingModel;
    private Integer chunkSize;
    private Integer chunkOverlap;
    private Integer status;
}
