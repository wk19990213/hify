# LLM 节点 MCP 工具调用设计

## 目标

工作流中的 AI 对话节点不再硬编码 API 调用，改为 LLM 自主判断是否调用 MCP 工具。工具执行结果自动传回 LLM，形成多轮工具调用循环。

## 设计决策

| 决策 | 选择 |
|------|------|
| 落点 | 改造现有 AI 对话(llm)节点 |
| 工具来源 | 默认继承 Agent 的工具绑定，可逐节点覆盖 |
| 最大循环轮次 | 3 轮 |
| 工具调用失败 | 把错误传给 LLM，让 LLM 决定如何应对 |

## 架构

### 调用流程

```
LLM 节点「查订单信息」
├─ 第 1 轮: LLM 判断需要查订单 → tool_call: 查订单(12345)
│          执行工具 → 返回订单数据
├─ 第 2 轮: LLM 判断需要查物流 → tool_call: 查物流(12345)
│          执行工具 → 返回物流数据
└─ 第 3 轮: LLM 信息齐全 → content: "您的订单预计明天送达"
            无 tool_call → 循环结束
```

### 改动模块

```
hify-provider/     ChatRequest +tools, ProviderAdapter +工具格式
hify-mcp/          McpClientManager + StdioTransport + SseTransport
hify-workflow/     LlmNodeExecutor +工具调用循环
hify-chat/         WorkflowRunReq +工具列表
hify-web/          WorkflowEditor +工具勾选 UI
```

## 消息拼接

每次 LLM 调用前，messages 包含历史所有轮次：

```
[system]  分析用户消息，有需要时调用工具，最后用自然语言回复
[user]    帮我查下订单 12345
[assistant(tool_calls)]  查订单({"orderId":"12345"})
[tool]    {"orderId":"12345","status":"已发货"}
[assistant(tool_calls)]  查物流({"orderId":"12345"})
[tool]    {"courier":"顺丰","status":"运输中"}
```

## 工具调用循环伪代码

```java
List<Map<String, String>> messages = new ArrayList<>();
messages.add(Map.of("role", "system", "content", prompt));
messages.add(Map.of("role", "user", "content", userMessage));

List<ToolDef> tools = resolveTools(node, agentTools);

for (int round = 1; round <= 3; round++) {
    ChatRequest req = new ChatRequest(modelId, messages, 0.7, false, tools);
    String response = adapter.chat(baseUrl, authConfig, req);
    
    String content = adapter.extractContent(response);
    List<ToolCall> toolCalls = adapter.extractToolCalls(response);
    
    if (toolCalls.isEmpty()) {
        return buildOutput(content);  // 循环结束
    }
    
    for (ToolCall tc : toolCalls) {
        ToolResult r = mcpClientManager.callTool(tc.name, tc.arguments);
        messages.add(Map.of("role", "assistant", "content", tc.toJson()));
        messages.add(Map.of("role", "tool", "content", r.toJson()));
    }
    
    if (content != null && !content.isBlank()) {
        return buildOutput(content);  // LLM 同时给出了回复和工具调用，取回复
    }
}
return buildOutput(extractContent(lastResponse));
```

## MCP 客户端运行时

### 自建精简实现

用 Java 标准库实现，零外部依赖，仅支持 MCP 协议中的两个操作：

- `tools/list` — 工具发现
- `tools/call` — 工具调用

### 组件

```
McpClientManager           // 进程/连接缓存，超时管理
├── McpClient              // JSON-RPC 编解码
│   ├── StdioTransport     // 子进程 stdin/stdout
│   └── SseTransport       // HTTP POST + SSE
```

### StdioTransport

```java
Process p = new ProcessBuilder(command, args).start();
// 通过 stdin 发送 JSON-RPC，从 stdout 读取响应
// 进程按需启动，空闲超时后关闭
```

### SseTransport

```java
HttpURLConnection conn = openConnection(url);
// POST 发送 JSON-RPC，读取响应体
```

## ProviderAdapter 改动

### ChatRequest 新增 tools

```java
public record ChatRequest(
    String model, List<Map<String, String>> messages,
    double temperature, boolean stream,
    List<ToolDef> tools          // 可为 null（纯对话）
) {}
```

### OpenAiAdapter

`buildOpenAiBody()` 加一行：`if (tools != null) body.put("tools", toOpenAiTools(tools));`

OpenAI 工具格式：
```json
{"type": "function", "function": {"name": "查订单", "description": "...", "parameters": {...}}}
```

### AnthropicAdapter

覆盖 `toOpenAiTools()` 转换为 Anthropic 格式（顶层的 `tools` 字段，特定内容块类型）。

### 响应解析

新增 `extractToolCalls(String response)` 方法，从 LLM 响应中提取工具调用列表。

## 前端改动

### WorkflowEditor.vue — LLM 节点配置

- 新增「启用工具调用」开关
- 开关打开后显示 Agent 绑定工具列表，可逐工具勾选
- 不勾选 = 纯对话节点（现有行为，向后兼容）

### 变量传递

Agent 触发工作流时，工具列表随 WorkflowRunReq 传入，LlmNodeExecutor 在执行时解析并构建 ToolDef。

## 验证

1. 启动一个已绑定订单查询 MCP 工具的 Agent，对话触发工作流
2. 工作流中 LLM 节点启用工具调用，勾选「查订单」工具
3. 发送"帮我查下订单 12345"，验证 LLM 自动调用工具并生成回复
4. 验证 3 轮上限（故意设计需要 > 3 轮查询的场景）
5. 验证工具调用失败时 LLM 能给出合理回复
