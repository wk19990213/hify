# Hify 架构

## 整体结构

模块化单体。一个 Spring Boot 应用，Maven 多模块组织。
模块间通过 Service 接口调用，不直接引用其他模块的实现类，为后续微服务拆分留口子。

## 模块划分

```
hify/
├── hify-app/            # 启动模块，Spring Boot Application
├── hify-provider/       # 模型提供商管理
├── hify-agent/          # Agent 管理与配置
├── hify-chat/           # 对话引擎
├── hify-mcp/            # MCP 工具管理与调用
├── hify-workflow/       # 工作流编排与执行
├── hify-knowledge/      # 知识库与 RAG
├── hify-common/         # 公共模块（工具类、常量、异常、DTO 基类）
├── hify-web/            # Vue 前端
└── deploy/              # Docker + K8s 部署配置
```

## 模块依赖关系

- hify-chat → hify-agent, hify-provider（对话时读取 Agent 配置和模型配置）
- hify-chat → hify-workflow（对话可能触发工作流）
- hify-chat → hify-knowledge（对话可能走 RAG 检索）
- hify-agent → hify-mcp（Agent 绑定工具）
- 所有业务模块 → hify-common
- hify-app → 所有业务模块（启动入口）

## 外部调用处理

- LLM 调用使用独立线程池（llmExecutor），和业务请求隔离
- Resilience4j 熔断，每个提供商独立熔断器
  - slidingWindowSize: 10
  - failureRateThreshold: 50%
  - waitDurationInOpenState: 30s
- 对话接口 60s 超时，连通性测试 10s 超时
- 重试策略按异常类型区分：网络抖动重试，认证失败不重试，限流退避重试
- 流式响应使用 SseEmitter + 独立线程池

## 缓存策略

- Provider / Agent 配置：Redis Cache-Aside，TTL 30min
- 对话上下文：Redis，TTL 2h，key = session:{sessionId}
- 对话消息持久化走 MySQL，不缓存
- MCP Server 配置数量少，不缓存

## 部署架构（当前 50 人规模）

Nginx（静态文件 + API 反向代理）→ Spring Boot → MySQL / Redis / pgvector / LLM API
- Nginx 配置 SSE 需要 proxy_buffering off
- 全部组件 Docker 镜像化，K8s 部署
- 本地开发直接 java -jar + npm run dev

## 扩展路径

1. chat 模块拆分独立部署，水平扩展
2. MySQL 读写分离
3. Redis 单实例 → 集群
4. 引入消息队列做异步处理（对话日志异步写入）

## MCP 服务架构

MCP 服务为独立 Spring Boot 应用，与 Hify 主应用完全分离部署，通过 HTTP JSON-RPC 通信（非 stdio 子进程）。

- **主应用侧**：`hify-mcp` 模块通过 `HttpJsonRpcTransport`（RestTemplate）调用 MCP 服务
- **服务侧**：`hify-mcp-services/` 目录下独立应用，例如 `order-service`（订单查询，端口 8090）
- Agent 绑定粒度：绑定整个 MCP 服务（非逐个工具），LLM 自主决定调用哪个工具
- 工具清单由 `McpClientManager.listTools(serverId)` 动态拉取，运行时注入 LLM 的 Function Calling
