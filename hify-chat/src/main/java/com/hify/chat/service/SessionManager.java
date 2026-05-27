package com.hify.chat.service;

import com.hify.chat.dto.ChatMessageResp;
import com.hify.chat.dto.ChatSessionResp;
import com.hify.chat.entity.ChatSessionEntity;

import java.util.List;

/**
 * 会话管理器 —— 会话 CRUD + 历史消息查询。
 */
public interface SessionManager {

    ChatSessionResp createSession(Long agentId, String title);

    ChatSessionResp getSessionDetail(Long sessionId);

    List<ChatSessionResp> listAgentSessions(Long agentId);

    List<ChatMessageResp> getHistory(Long sessionId);

    void endSession(Long sessionId);

    void deleteSession(Long sessionId);

    /** 获取会话实体（内部使用，校验存在、未删除、未结束） */
    ChatSessionEntity getSession(Long sessionId);

    /** 取用户消息前 20 字作为会话标题（首次发送时自动设置） */
    void autoTitle(ChatSessionEntity session, String userContent);
}
