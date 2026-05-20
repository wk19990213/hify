package com.hify.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hify.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("document")
public class DocumentEntity extends BaseEntity {
    private Long kbId;
    private String name;
    private String fileType;
    private Long fileSize;
    private String status;
    private Integer chunkCount;
}
