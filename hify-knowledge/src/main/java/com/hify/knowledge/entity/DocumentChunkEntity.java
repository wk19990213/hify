package com.hify.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hify.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "document_chunk", autoResultMap = true)
public class DocumentChunkEntity extends BaseEntity {
    private Long docId;
    private Integer chunkIndex;
    private String content;
    private String embedding;
}
