package com.hify.chat.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hify.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_session")
public class ChatSessionEntity extends BaseEntity {

    private String sessionId;
    private Long agentId;
    private String title;
    private String status;
    private String contextJson;
}
