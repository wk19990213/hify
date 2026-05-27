package com.hify.chat.service;

import com.hify.chat.dto.AgentContext;
import com.hify.mcp.mcp.ToolDef;
import java.util.List;
import java.util.Map;

/**
 * 对话编排器 — 负责 LLM 调用 + Function Calling 工具循环 + 流式 SSE 推送。
 * 不做消息持久化和上下文解析，由 Caller 预先构建 messages/tools 传入。
 */
public interface ChatOrchestrator {

    /**
     * 非流式 LLM 调用（含工具调用循环，最多 3 轮）。
     * @param ctx   Agent 调用上下文
     * @param tools 可用的工具列表，null 表示无工具
     * @param messages 消息列表（含 system + 历史 + 用户消息）
     * @return 最终 assistant 文本内容
     */
    ChatResult execute(AgentContext ctx, List<ToolDef> tools, List<Map<String, Object>> messages);

    /**
     * 流式 LLM 调用（先执行工具循环，再流式推送最终回复）。
     * @param ctx      Agent 调用上下文
     * @param tools    可用的工具列表，null 表示无工具
     * @param messages 消息列表
     * @param handler  流式事件处理器
     */
    void executeStream(AgentContext ctx, List<ToolDef> tools, List<Map<String, Object>> messages,
                       StreamEventHandler handler);
}
