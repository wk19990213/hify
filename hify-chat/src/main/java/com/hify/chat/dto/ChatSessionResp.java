package com.hify.chat.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ChatSessionResp {
    private Long sessionId;
    private String sessionUuid;
    private Long agentId;
    private String title;
    private String status;
    private List<ChatMessageResp> messages;
    private LocalDateTime createdAt;
}
