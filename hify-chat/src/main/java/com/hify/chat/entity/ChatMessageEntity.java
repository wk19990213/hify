package com.hify.chat.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hify.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_message")
public class ChatMessageEntity extends BaseEntity {

    private Long sessionId;
    private String role;
    private String content;
    private Integer tokenCount;
    private String metadataJson;
    private String finishReason;
    private Integer latencyMs;
}
