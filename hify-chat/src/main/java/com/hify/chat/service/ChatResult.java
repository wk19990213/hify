package com.hify.chat.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** LLM 调用结果 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResult {
    private String content;
    private String finishReason;
    private int tokenCount;
}
