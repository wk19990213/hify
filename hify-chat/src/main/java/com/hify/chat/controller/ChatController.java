package com.hify.chat.controller;

import com.hify.chat.dto.ChatMessageResp;
import com.hify.chat.dto.ChatSessionResp;
import com.hify.chat.dto.SendMessageReq;
import com.hify.chat.service.ChatService;
import com.hify.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/sessions")
    public Result<ChatSessionResp> createSession(@RequestParam("agentId") Long agentId,
                                                  @RequestParam(name = "title", defaultValue = "新对话") String title) {
        return Result.ok(chatService.createSession(agentId, title));
    }

    @GetMapping("/sessions")
    public Result<List<ChatSessionResp>> listSessions(@RequestParam("agentId") Long agentId) {
        return Result.ok(chatService.listAgentSessions(agentId));
    }

    @GetMapping("/sessions/{sessionId}")
    public Result<ChatSessionResp> getSession(@PathVariable("sessionId") Long sessionId) {
        return Result.ok(chatService.getSessionDetail(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/messages")
    public Result<ChatMessageResp> sendMessage(@PathVariable("sessionId") Long sessionId,
                                                @RequestBody SendMessageReq req) {
        return Result.ok(chatService.sendMessage(sessionId, req));
    }

    @PostMapping("/sessions/{sessionId}/stream")
    public SseEmitter sendMessageStream(@PathVariable("sessionId") Long sessionId,
                                         @RequestBody SendMessageReq req) {
        return chatService.sendMessageStream(sessionId, req);
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public Result<List<ChatMessageResp>> getHistory(@PathVariable("sessionId") Long sessionId) {
        return Result.ok(chatService.getHistory(sessionId));
    }

    @PutMapping("/sessions/{sessionId}/end")
    public Result<Void> endSession(@PathVariable("sessionId") Long sessionId) {
        chatService.endSession(sessionId);
        return Result.ok();
    }

    @DeleteMapping("/sessions/{sessionId}")
    public Result<Void> deleteSession(@PathVariable("sessionId") Long sessionId) {
        chatService.deleteSession(sessionId);
        return Result.ok();
    }
}
