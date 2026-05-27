package com.hify.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hify.agent.service.AgentService;
import com.hify.chat.dto.ChatMessageResp;
import com.hify.chat.dto.ChatSessionResp;
import com.hify.chat.entity.ChatMessageEntity;
import com.hify.chat.entity.ChatSessionEntity;
import com.hify.chat.mapper.ChatMessageMapper;
import com.hify.chat.mapper.ChatSessionMapper;
import com.hify.chat.service.SessionManager;
import com.hify.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionManagerImpl implements SessionManager {

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    private final AgentService agentService;

    @Override
    @Transactional
    public ChatSessionResp createSession(Long agentId, String title) {
        agentService.getDetail(agentId); // 校验 Agent 存在
        ChatSessionEntity session = new ChatSessionEntity();
        session.setSessionId(UUID.randomUUID().toString());
        session.setAgentId(agentId);
        session.setTitle(title != null ? title : "新对话");
        session.setStatus("active");
        sessionMapper.insert(session);
        return toSessionResp(session, List.of());
    }

    @Override
    public ChatSessionResp getSessionDetail(Long sessionId) {
        ChatSessionEntity session = getSession(sessionId);
        List<ChatMessageResp> messages = getHistory(sessionId);
        return toSessionResp(session, messages);
    }

    @Override
    public List<ChatSessionResp> listAgentSessions(Long agentId) {
        List<ChatSessionEntity> sessions = sessionMapper.selectList(
                new LambdaQueryWrapper<ChatSessionEntity>()
                        .eq(ChatSessionEntity::getAgentId, agentId)
                        .eq(ChatSessionEntity::getStatus, "active")
                        .eq(ChatSessionEntity::getDeleted, 0)
                        .ge(ChatSessionEntity::getCreatedAt, java.time.LocalDateTime.now().minusMonths(1))
                        .orderByDesc(ChatSessionEntity::getCreatedAt)
        );
        return sessions.stream()
                .map(s -> toSessionResp(s, List.of()))
                .toList();
    }

    @Override
    public List<ChatMessageResp> getHistory(Long sessionId) {
        return messageMapper.selectList(
                        new LambdaQueryWrapper<ChatMessageEntity>()
                                .eq(ChatMessageEntity::getSessionId, sessionId)
                                .eq(ChatMessageEntity::getDeleted, 0)
                                .orderByAsc(ChatMessageEntity::getCreatedAt))
                .stream()
                .map(this::toMessageResp)
                .toList();
    }

    @Override
    @Transactional
    public void endSession(Long sessionId) {
        ChatSessionEntity session = getSession(sessionId);
        session.setStatus("ended");
        sessionMapper.updateById(session);
    }

    @Override
    @Transactional
    public void deleteSession(Long sessionId) {
        getSession(sessionId); // 校验存在
        messageMapper.delete(new LambdaQueryWrapper<ChatMessageEntity>()
                .eq(ChatMessageEntity::getSessionId, sessionId));
        sessionMapper.deleteById(sessionId);
    }

    @Override
    public ChatSessionEntity getSession(Long sessionId) {
        ChatSessionEntity session = sessionMapper.selectById(sessionId);
        if (session == null || session.getDeleted() == 1) {
            throw BizException.notFound("会话不存在");
        }
        if ("ended".equals(session.getStatus())) {
            throw BizException.paramError("会话已结束");
        }
        return session;
    }

    @Override
    public void autoTitle(ChatSessionEntity session, String userContent) {
        if (!"新对话".equals(session.getTitle())) return;
        String title = userContent.length() > 20 ? userContent.substring(0, 20) + "..." : userContent;
        title = title.replace('\n', ' ').replace('\r', ' ');
        session.setTitle(title);
        sessionMapper.updateById(session);
    }

    private ChatMessageResp toMessageResp(ChatMessageEntity entity) {
        ChatMessageResp resp = new ChatMessageResp();
        BeanUtils.copyProperties(entity, resp);
        return resp;
    }

    private ChatSessionResp toSessionResp(ChatSessionEntity s, List<ChatMessageResp> msgs) {
        ChatSessionResp resp = new ChatSessionResp();
        resp.setSessionId(s.getId());
        resp.setSessionUuid(s.getSessionId());
        resp.setAgentId(s.getAgentId());
        resp.setTitle(s.getTitle());
        resp.setStatus(s.getStatus());
        resp.setMessages(msgs);
        resp.setCreatedAt(s.getCreatedAt());
        return resp;
    }
}
