package com.hify.chat.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatMessageResp {
    private Long id;
    private String role;
    private String content;
    private Integer tokenCount;
    private String finishReason;
    private Integer latencyMs;
    private LocalDateTime createdAt;
}
