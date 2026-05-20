package com.hify.knowledge.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DocumentResp {
    private Long id;
    private Long kbId;
    private String name;
    private String fileType;
    private Long fileSize;
    private String status;
    private Integer chunkCount;
    private LocalDateTime createdAt;
}
