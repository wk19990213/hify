package com.hify.chat.service;

import com.hify.chat.dto.ChatMessageResp;
import com.hify.chat.dto.ChatSessionResp;
import com.hify.chat.dto.SendMessageReq;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface ChatService {

    ChatSessionResp createSession(Long agentId, String title);

    ChatMessageResp sendMessage(Long sessionId, SendMessageReq req);

    SseEmitter sendMessageStream(Long sessionId, SendMessageReq req);

    List<ChatMessageResp> getHistory(Long sessionId);

    void endSession(Long sessionId);
}
