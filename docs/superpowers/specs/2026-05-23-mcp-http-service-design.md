# MCP 服务 HTTP 化 & Agent 服务级绑定设计

> 设计日期：2026-05-23

## 目标

1. MCP 服务改为独立 Spring Boot 应用部署，HTTP JSON-RPC 通信（不再使用 Python stdio 子进程）
2. Agent 绑定粒度从「单个工具」改为「整个 MCP 服务」
3. 订单查询从硬编码 MockOrderController 改为独立 MCP 服务
4. 配置了知识库/工作流的 Agent 由 LLM 自主决定是否调用工具（标准 Function Calling 模式）

---

## 架构

```
┌─────────────────────────────────────────────────────┐
│                    Hify 主应用                        │
│                                                      │
│  Agent ──绑定──► MCP 服务 A（订单查询）               │
│       ──绑定──► MCP 服务 B（天气查询）               │
│                                                      │
│  对话 / 工作流执行时：                                │
│  ┌──────────────────────────────────────────────┐    │
│  │ resolveAgentTools(agentId)                   │    │
│  │   → 遍历绑定的 MCP 服务                       │    │
│  │   → mcpClientManager.listTools(serverId)      │    │
│  │   → 汇总全部 ToolDef 传入 LLM                  │    │
│  │   → LLM 自主决定调不调、调哪个                 │    │
│  └──────────────────────────────────────────────┘    │
│                                                      │
│  McpClientManager ──HTTP JSON-RPC──►  MCP 服务 A     │
│  (HttpJsonRpcTransport)            (Spring Boot)     │
│                                                      │
└─────────────────────────────────────────────────────┘
```

- Agent 绑定整个 MCP 服务，非逐个工具
- LLM 节点保留 `toolsEnabled` 开关控制是否启用工具，工具清单由 Agent 统一注入（不再手填白名单）
- 去掉 stdio 传输方式，统一 HTTP

---

## 数据模型变更

### mcp_server 表

去掉 stdio 字段，统一 HTTP：

| 字段 | 类型 | 变更 | 说明 |
|---|---|---|---|
| id | bigint | 保留 | |
| name | varchar(64) | 保留 | 服务名称，唯一 |
| command | varchar | **删除** | 不再需要命令行 |
| args_json | varchar | **删除** | 不再需要参数列表 |
| env_vars_json | varchar | **删除** | 不再需要环境变量 |
| url | varchar(255) | **修改** | 改为 NOT NULL，MCP 服务 HTTP 地址 |
| auth_config | json | **新增** | 鉴权配置（Bearer Token / API Key 等），对标 provider.auth_config |
| transport_type | varchar(32) | **修改** | 固定为 `http` |
| status | tinyint(1) | 保留 | 0=禁用，1=启用 |
| created_at | datetime | 保留 | |
| updated_at | datetime(3) | 保留 | |
| deleted | tinyint(1) | 保留 | 逻辑删除 |

### agent_mcp_server 表（替代 agent_tool）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| agent_id | bigint | Agent ID |
| mcp_server_id | bigint | MCP 服务 ID |
| sort_order | int | 排序 |
| created_at | datetime(3) | 创建时间 |
| updated_at | datetime(3) | 更新时间 |
| deleted | tinyint(1) | 逻辑删除：0=正常，1=已删除 |

- 唯一约束：`uk_agent_server (agent_id, mcp_server_id, deleted)`
- 与旧 `agent_tool` 的区别：不再存储 `tool_name`、`tool_type`、`config_json`。绑定即授权该服务的全部工具。

### 工作流 LLM 节点 config.json

保留 `toolsEnabled` 开关（默认 false），删除手填 `tools` 白名单。Agent 绑定的全部工具由引擎统一注入，节点只需决定开/关：

```json
{
  "prompt": "...",
  "maxRetries": 0,
  "toolsEnabled": false
}
```

当 `toolsEnabled=true` 时，该 LLM 节点可使用 Agent 已绑定的全部 MCP 工具；为 false 时仅做纯文本生成。

---

## 订单查询 MCP 服务

独立 Spring Boot 应用，与 Hify 完全分离部署。

### 基本信息

- 应用名：`order-mcp-service`
- 端口：`8090`
- 端点：`POST /mcp`（JSON-RPC 2.0）
- 依赖：仅 `spring-boot-starter-web`，无数据库/MyBatis/Redis

### 项目结构

```
hify-mcp-services/
└── order-service/
    ├── pom.xml
    └── src/main/java/com/hify/order/
        ├── OrderServiceApplication.java
        ├── controller/McpController.java    # POST /mcp
        ├── service/OrderService.java        # 订单查询逻辑
        └── dto/JsonRpcRequest.java / JsonRpcResponse.java
```

### 支持的方法

| 方法 | 参数 | 返回 |
|---|---|---|
| `initialize` | protocolVersion, clientInfo | protocolVersion, capabilities.tools, serverInfo |
| `tools/list` | page, pageSize | tools[], total, page, pageSize |
| `tools/call` | name, arguments | content[{type, text}] |

### 工具定义

- `query_order(orderId: string)` — 根据订单号查详情
- `list_orders(status?: string)` — 列出订单，可选状态过滤

### 数据

3 条硬编码订单（从 MockOrderController 迁移）：

| 订单号 | 客户 | 状态 | 金额 |
|---|---|---|---|
| 12345 | 张三 | 已发货 | 497.00 |
| 67890 | 李四 | 待发货 | 1999.00 |
| 11111 | 王五 | 已签收 | 1500.00 |

---

## Hify 主应用变更

### 模块变更清单

| 模块 | 文件 | 变更 |
|---|---|---|
| hify-agent | AgentToolEntity → AgentMcpServerEntity | 重命名，字段简化 |
| hify-agent | AgentToolRequest → AgentMcpServerRequest | 同上 |
| hify-agent | AgentToolResponse → AgentMcpServerResponse | 同上 |
| hify-agent | AgentServiceImpl | 保存/查询服务绑定 |
| hify-chat | ChatServiceImpl.resolveAgentTools() | 遍历服务 ID 拉取工具 |
| hify-chat | ChatServiceImpl.sendMessage() | **新增** Function Calling 循环（非工作流路径） |
| hify-chat | ChatServiceImpl.sendMessageStream() | **新增** Function Calling 循环（非工作流路径） |
| hify-workflow | LlmNodeExecutor | 删除 resolveNodeTools()，改为检查 toolsEnabled |
| hify-mcp | StdioTransport | **删除** |
| hify-mcp | SseTransport → HttpJsonRpcTransport | **重命名**，改用 WebClient 连接池 |
| hify-mcp | McpServerEntity | 删除 command/argsJson/envVarsJson |
| hify-mcp | McpServerCreateReq/UpdateReq/Resp | 同步字段调整 |
| hify-app | MockOrderController | **删除** |
| hify-web | AgentList.vue | 工具勾选框 → 服务勾选框 |
| hify-web | WorkflowEditor.vue | LLM 节点移除手填工具名称输入框，保留 toolsEnabled 开关 |
| hify-web | McpServerList.vue | 表单去掉 command/args 字段 |
| hify-web | api/mcpServer.ts | 更新类型定义 |

### 移除/重命名的模块/文件

- `hify-mcp/mcp-servers/order-server/` — Python MCP 服务
- `hify-mcp/src/.../StdioTransport.java` — stdio 传输
- `hify-mcp/src/.../SseTransport.java` → `HttpJsonRpcTransport.java` — 重命名 + 连接池改造
- `hify-app/.../MockOrderController.java` — 硬编码订单接口

---

## 数据流

### 场景 A：知识库 Agent（无工作流）

```
用户发消息
  → ChatServiceImpl.sendMessage()
  → resolveAgentTools(agentId): 遍历绑定的 MCP 服务，拉取全部工具
  → buildMessages(): system prompt + KB 检索 + 历史 + 当前消息
  → adapter.chat(ChatRequest with tools)
  → LLM 自主判断是否调工具
     ├─ 不调 → 直接返回文本
     └─ 调工具 → McpClientManager.callTool() → HTTP POST 到 MCP 服务
        → 工具结果追加到 messages → 再次调 LLM 生成最终回复
```

### 场景 B：工作流 Agent

```
用户发消息
  → ChatServiceImpl.sendMessage()
  → resolveAgentTools(agentId): 拉取全部工具
  → WorkflowRunReq.tools = 全部工具
  → WorkflowEngine 执行 DAG
     → LLM 节点 → LlmNodeExecutor.execute(ctx)
     → 检查节点 config.toolsEnabled：
        ├─ false → 不传工具，纯文本生成
        └─ true → ctx.getTools() 全部传入 ChatRequest
     → LLM 自主决定调不调工具（最多 3 轮 Function Calling 循环）
  → 提取工作流最终输出
```

---

## 数据迁移

从 `agent_tool`（按工具名绑定）迁移到 `agent_mcp_server`（按服务绑定）：

1. 创建 `agent_mcp_server` 表（不改动 agent_tool）
2. 执行迁移 SQL：
```sql
INSERT INTO agent_mcp_server (agent_id, mcp_server_id, sort_order, created_at, updated_at)
SELECT DISTINCT agent_id, mcp_server_id, 0, NOW(3), NOW(3)
FROM agent_tool
WHERE tool_type = 'mcp' AND mcp_server_id IS NOT NULL AND deleted = 0;
```
3. 后端同时读取两张表（灰度期），新写入只写新表
4. 确认稳定后删除 `agent_tool` 表

---

## 可靠性保障

### 健康检查

`McpClientManager` 增加 `healthCheck(serverId)` 方法：发送 `tools/list` 请求，超时 5s。管理后台 MCP 服务列表页展示健康状态（对标 `provider_health` 模式）。

### 鉴权

`mcp_server` 表增加 `auth_config JSON` 字段，与 `provider.auth_config` 一致。`HttpJsonRpcTransport` 发送请求时读取 `auth_config`，注入对应 HTTP Header（如 `Authorization: Bearer xxx` 或自定义 Header）。

### 熔断保护

为每个 MCP 服务创建独立的 Resilience4j 熔断器，参数与 LLM 调用一致：
- slidingWindowSize: 10
- failureRateThreshold: 50%
- waitDurationInOpenState: 30s

### 日志埋点

MCP 工具调用前后打 INFO 日志：
```java
log.info("MCP tool call start: serverId={}, tool={}, args={}", serverId, toolName, arguments);
// ... 调用 ...
log.info("MCP tool call done: serverId={}, tool={}, cost={}ms, success={}", serverId, toolName, cost, result.isSuccess());
```

### HTTP 连接池

`HttpJsonRpcTransport` 使用 `RestTemplate` + `PoolingHttpClientConnectionManager`（或项目已有的 WebClient），配置 `maxTotal=50, maxPerRoute=10`，避免每次调用新建 TCP 连接。

---

## 验证清单

1. 启动订单 MCP 服务（`java -jar order-service.jar`），`POST /mcp` 可正常响应 JSON-RPC
2. Hify 管理后台创建 MCP 服务，填入 `http://localhost:8090/mcp`，状态启用，可看到健康状态
3. Agent 编辑页可看到 MCP 服务列表（而非工具列表），勾选绑定
4. 知识库 Agent 对话：发「查订单 12345」，LLM 自主调用 `query_order` 工具返回结果
5. 工作流 LLM 节点：`toolsEnabled=false` 时不调工具，`toolsEnabled=true` 时 LLM 自主决定
6. MockOrderController 已删除，`/v1/mock/orders` 不再存在
7. Agent 工具绑定从 agent_tool 迁移到 agent_mcp_server
8. MCP 订单服务关闭后，Hify 调用工具返回 `success=false` 且不阻塞主流程
