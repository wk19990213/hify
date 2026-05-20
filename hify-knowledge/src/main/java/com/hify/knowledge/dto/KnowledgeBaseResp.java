package com.hify.knowledge.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class KnowledgeBaseResp {
    private Long id;
    private String name;
    private String description;
    private String embeddingModel;
    private Integer chunkSize;
    private Integer chunkOverlap;
    private Integer status;
    private Integer documentCount;
    private LocalDateTime createdAt;
}
