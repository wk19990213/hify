# Hify

一个简版 AI Agent 开发平台，可本地部署，面向团队内部小规模使用。提供模型提供商管理、Agent 配置、对话引擎、RAG 知识库、工作流编排和 MCP 工具接入。

## Language

### 模型供给

**Provider** (提供商):
一个 LLM 服务方，配置了 API 地址、密钥、超时等连接参数。例如 "OpenAI Official"、"Anthropic"、"硅基流动"。
_Avoid_: 服务商、模型供应商、LLM

**ModelConfig** (模型配置):
一个抽象模型定义，不绑定具体提供商。Agent 绑定时选择的是 ModelConfig 而非 Provider。包含模型名称、能力标签、价格配置。`provider_id` 指向首次导入该模型的代表提供商，`provider_count` 记录有多少个 Provider 提供了该模型——归零时 ModelConfig 被删除。
_Avoid_: 模型、model、模板

**ProviderModel** (供给关系):
连接 Provider 和 ModelConfig 的多对多中间表。同步模型列表时自动维护。表示"某某 Provider 提供了某某模型"。
_Avoid_: 关联、绑定

### 模型适配

**ProviderAdapter** (提供商适配器):
按 Provider 类型（OPENAI / ANTHROPIC / OLLAMA / OPENAI_COMPATIBLE）执行 LLM 调用的策略接口。负责构建 API URL、鉴权 Header、解析流式/非流式响应。
_Avoid_: 连接器、driver

**OPENAI_COMPATIBLE**:
兼容 OpenAI API 协议的第三方服务类型，用于接入国内模型服务（DeepSeek、硅基流动等）。

### 连接健康

**ProviderHealth** (健康状态):
Provider 的运行状况快照。状态为四态：

| 状态 | 含义 |
|------|------|
| UNKNOWN | 尚未执行健康检查 |
| HEALTHY | 健康检查通过 |
| DEGRADED | 熔断器打开，触发降级 |
| UNHEALTHY | 健康检查失败，服务不可达 |

_Avoid_: 在线状态、可用性

### Agent 与工具

**Agent**:
用户创建的 AI 配置单元。绑定一个 ModelConfig，设置 System Prompt、temperature、对话轮次上限，可关联一个 KnowledgeBase 和多个工具。用户与 Agent 对话。
_Avoid_: Bot、ChatBot、Assistant、机器人

**AgentTool** (工具绑定):
Agent 与具体工具的关联关系。工具分为两种类型：
- **MCP 工具**— 通过 McpServer 接入的外部工具（主要扩展方式）
- **内置工具** — 平台自带的工具，由 configJson 配置

_Avoid_: 插件、能力、技能

**McpServer** (MCP 服务器):
MCP 协议的外部工具服务器，支持三种传输方式：stdio（进程通信）、sse（服务器推送）、streamable-http（流式 HTTP）。Agent 通过 AgentTool 绑定它。
_Avoid_: 工具服务、MCP 服务

### 对话

**ChatSession** (会话):
一次对话的容器。绑定一个 Agent，有 UUID 标识和标题（自动从首条用户消息生成）。生命周期由用户手动控制（创建 → active → 手动结束 → ended）。
_Avoid_: 对话、聊天、conversation

**ChatMessage** (消息):
对话中的单条消息。role 分 user / assistant / system / tool。记录 token 消耗、LLM finish reason、响应延迟。
_Avoid_: 消息记录、回复

**上下文解析链** (Context Resolution):
发送消息时确定调用哪个 LLM 的链路：Agent → ModelConfig → ProviderModel → Provider → ProviderAdapter → 解密 authConfig。

### RAG 知识库

**KnowledgeBase** (知识库):
一个 RAG 知识容器。配置了嵌入模型（默认 BAAI/bge-m3）、分块大小、分块重叠参数。Agent 可关联一个 KnowledgeBase 实现文档问答。
_Avoid_: 知识库、知识空间

**Document** (文档):
上传到 KnowledgeBase 的单个文件（PDF/DOCX/TXT）。上传后进入 processing 状态，切分并向量化后变为 completed。
_Avoid_: 资料、附件

**DocumentChunk** (文档块):
Document 切分后的文本片段，携带向量嵌入。用于余弦相似度检索。检索策略为纯向量检索（非混合检索）。
_Avoid_: 片段、切片、分块

### 工作流

**Workflow** (工作流):
由节点和连线组成的执行流程，支持顺序执行和条件分支。Chat 对话可触发 Workflow 执行。
_Avoid_: 流程、流水线

**WorkflowNode** (节点):
工作流中的一个执行步骤。
_Avoid_: 步骤、stage

**WorkflowEdge** (连线):
两个节点之间的有向连接，决定流转方向。
_Avoid_: 连线、连接

## Flagged ambiguities

无。MCP 和 Workflow 模块当前为骨架实现，待实现后再明确细化。

## Example dialogue

> **Dev**: 我建了一个 Agent，绑了 GPT-4 Turbo，怎么对话的时候报 Provider UNHEALTHY？
>
> **Domain expert**: 先查 ProviderHealth。如果状态是 UNHEALTHY，说明健康检查失败了，API 不可达。检查 Provider 的 baseUrl、密钥是否正确，然后用 test-connection 验证。
>
> **Dev**: 我的 OpenAI 密钥换过，改完之后 Provider 变 HEALTHY 了，但还是报错？
>
> **Agent 专家**: Agent 绑定的是 ModelConfig，不是直接绑定 Provider。看一下 Agent 当前绑定的 ModelConfig 是什么——它的 `provider_id` 指向的是哪个 Provider？也许你的 ModelConfig 指向了另一个已被删除或失效的 Provider。
>
> **Dev**: 明白了，这个 ModelConfig 的 `provider_id` 指向了旧的 Provider，`provider_count` 已经有新 Provider 的记录了。我该直接改 `provider_id` 吗？
>
> **Agent 专家**: 不用手动改。ModelConfig 的 `provider_id` 是首次导入该模型的 Provider，不影响 Agent 调用。实际调用走的是上下文解析链：Agent → ModelConfig → ProviderModel → Provider。只要 ProviderModel 能关联到一个 HEALTHY 的 Provider 就行。如果旧 Provider 不用了，直接删除它，系统会自动重新分配代表 Provider。
