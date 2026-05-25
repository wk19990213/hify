# Hify 项目规范
永远都要记住，你只能对本项目的文件进行操作，不可以操作本项目以外的文件
## 项目概述

Hify 是一个简版的 AI Agent 开发平台（参考 Dify），可本地部署，面向团队内部小规模使用（20-50 人同时在线）。

技术栈：Spring Boot 3.x + MyBatis-Plus + MySQL 8.x + Redis 7.x + pgvector
前端：Vue 3 + TypeScript + Element Plus + Vite
容器化：Docker + K8s

### 做什么
- 多模型提供商管理（OpenAI、Claude、Gemini、Ollama）
- Agent 创建与配置（绑模型、绑工具、System Prompt）
- 对话引擎（流式响应、多轮对话、上下文管理）
- RAG 知识库（文档上传、向量检索、引用回答）
- 简版工作流编排（顺序执行、条件分支）
- MCP 工具接入（Agent 可调用外部工具）
- 管理控制台（前端界面）

### 不做什么
- 不做可视化工作流拖拽编排
- 不做多租户 / 权限体系
- 不做插件市场、计费系统
- 不做消息推送、WebSocket 长连接（用 SSE 替代）

---

## 架构

### 整体结构
模块化单体。一个 Spring Boot 应用，Maven 多模块组织。
模块间通过 Service 接口调用，不直接引用其他模块的实现类，为后续微服务拆分留口子。

### 模块划分
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

### 模块依赖关系
- hify-chat → hify-agent, hify-provider（对话时读取 Agent 配置和模型配置）
- hify-chat → hify-workflow（对话可能触发工作流）
- hify-chat → hify-knowledge（对话可能走 RAG 检索）
- hify-agent → hify-mcp（Agent 绑定工具）
- 所有业务模块 → hify-common
- hify-app → 所有业务模块（启动入口）

### 外部调用处理
- LLM 调用使用独立线程池（llmExecutor），和业务请求隔离
- Resilience4j 熔断，每个提供商独立熔断器
  - slidingWindowSize: 10
  - failureRateThreshold: 50%
  - waitDurationInOpenState: 30s
- 对话接口 60s 超时，连通性测试 10s 超时
- 重试策略按异常类型区分：网络抖动重试，认证失败不重试，限流退避重试
- 流式响应使用 SseEmitter + 独立线程池

### 缓存策略
- Provider / Agent 配置：Redis Cache-Aside，TTL 30min
- 对话上下文：Redis，TTL 2h，key = session:{sessionId}
- 对话消息持久化走 MySQL，不缓存
- MCP Server 配置数量少，不缓存

### 部署架构（当前 50 人规模）
Nginx（静态文件 + API 反向代理）→ Spring Boot → MySQL / Redis / pgvector / LLM API
- Nginx 配置 SSE 需要 proxy_buffering off
- 全部组件 Docker 镜像化，K8s 部署
- 本地开发直接 java -jar + npm run dev

### 扩展路径（几千人规模，当前不做但不堵死）
1. chat 模块拆分独立部署，水平扩展
2. MySQL 读写分离
3. Redis 单实例 → 集群
4. 引入消息队列做异步处理（对话日志异步写入）

---

## 代码组织

### 后端模块内部结构
每个业务模块统一结构：
```
src/main/java/com/hify/{module}/
├── controller/        # REST 接口，只做参数校验和调用 Service
├── service/           # 业务逻辑接口
├── service/impl/      # 业务逻辑实现
├── mapper/            # MyBatis-Plus Mapper
├── entity/            # 数据库实体
├── dto/               # 请求/响应对象
├── config/            # 模块级配置类
├── exception/         # 模块级自定义异常
└── constant/          # 模块级常量
```

### 分层规则
- Controller 只做参数校验和调用 Service，不写业务逻辑
- Service 处理所有业务逻辑
- 跨模块调用走 Service 接口，不直接引用其他模块的 Mapper 或 Entity
- 公共工具类、基类放 hify-common

### 前端结构
```
hify-web/src/
├── api/               # 接口调用，按模块分文件
├── components/        # 公共组件
├── composables/       # 组合式函数
├── router/            # 路由配置
├── types/             # TypeScript 类型定义
├── utils/             # 工具函数（request.ts 等）
├── views/             # 页面组件，按模块分目录
│   ├── provider/
│   ├── agent/
│   └── chat/
└── App.vue
```

---

## 数据库规范

- 表名：小写下划线，不加前缀。例如 provider、chat_message
- 字段名：小写下划线。例如 api_key、base_url
- 主键：统一用 id，bigint 自增
- 时间字段：created_at、updated_at，datetime 类型
- 逻辑删除：deleted，tinyint，0=正常 1=删除
- 索引命名：idx_{表名}_{字段名}
- 所有外键在应用层维护，不建数据库级外键约束
- 字符集：utf8mb4
- 大表预判：chat_message 增长最快，提前考虑分页查询性能
- 分页查询避免 SELECT COUNT(*)，用游标分页或估算总数

### 核心数据模型
```
provider (模型提供商)
  └── model_config (模型配置) [1:N]

agent (Agent 配置)
  ├── → model_config [N:1]
  ├── ↔ mcp_server (MCP 服务) [M:N, 通过 agent_mcp_server，绑整个服务非单个工具]
  └── → chat_session (对话会话) [1:N]
        └── → chat_message (对话消息) [1:N]

workflow (工作流定义)
  ├── → workflow_node (节点) [1:N]
  │     └── LLM 节点 configJson.toolsEnabled 控制是否启用工具调用
  └── → workflow_edge (连线) [1:N]

knowledge_base (知识库)
  └── → document (文档) [1:N]
        └── → document_chunk (文档分块) [1:N]
```

### MCP 服务架构

MCP 服务为独立 Spring Boot 应用，与 Hify 主应用完全分离部署，通过 HTTP JSON-RPC 通信（非 stdio 子进程）。

- **主应用侧**：`hify-mcp` 模块通过 `HttpJsonRpcTransport`（RestTemplate）调用 MCP 服务
- **服务侧**：`hify-mcp-services/` 目录下独立应用，例如 `order-service`（订单查询，端口 8090）
- Agent 绑定粒度：绑定整个 MCP 服务（非逐个工具），LLM 自主决定调用哪个工具
- 工具清单由 `McpClientManager.listTools(serverId)` 动态拉取，运行时注入 LLM 的 Function Calling

具体表字段详见 docs/er-diagram.md

---

## 接口规范

### 路径
RESTful 风格：/api/v1/{资源复数名}
```
GET    /api/v1/providers          # 列表（分页）
POST   /api/v1/providers          # 创建
GET    /api/v1/providers/{id}     # 详情
PUT    /api/v1/providers/{id}     # 更新
DELETE /api/v1/providers/{id}     # 删除
POST   /api/v1/providers/{id}/test-connection  # 非 CRUD 操作用动词
```


## Workflow Orchestration

### 1. Plan Node Default
- Enter plan mode for ANY non-trivial task (3+ steps or architectural decisions)
- If something goes sideways, STOP and re-plan immediately - don't keep pushing
- Use plan mode for verification steps, not just building
- Write detailed specs upfront to reduce ambiguity

### 2. Subagent Strategy
- Use subagents liberally to keep main context window clean
- Offload research, exploration, and parallel analysis to subagents
- For complex problems, throw more compute at it via subagents
- One tack per subagent for focused execution

### 3. Self-Improvement Loop
- After ANY correction from the user: update `tasks/lessons.md` with the pattern
- Write rules for yourself that prevent the same mistake
- Ruthlessly iterate on these lessons until mistake rate drops
- Review lessons at session start for relevant project

### 4. Verification Before Done
- Never mark a task complete without proving it works
- Diff behavior between main and your changes when relevant
- Ask yourself: "Would a staff engineer approve this?"
- Run tests, check logs, demonstrate correctness

### 5. Demand Elegance (Balanced)
- For non-trivial changes: pause and ask "is there a more elegant way?"
- If a fix feels hacky: "Knowing everything I know now, implement the elegant solution"
- Skip this for simple, obvious fixes - don't over-engineer
- Challenge your own work before presenting it

### 6. Autonomous Bug Fixing
- When given a bug report: just fix it. Don't ask for hand-holding
- Point at logs, errors, failing tests - then resolve them
- Zero context switching required from the user
- Go fix failing CI tests without being told how

## Task Management

1. **Plan First**: Write plan to `tasks/todo.md` with checkable items
2. **Verify Plan**: Check in before starting implementation
3. **Track Progress**: Mark items complete as you go
4. **Explain Changes**: High-level summary at each step
5. **Document Results**: Add review section to `tasks/todo.md`
6. **Capture Lessons**: Update `tasks/lessons.md` after corrections

## Core Principles

- **Simplicity First**: Make every change as simple as possible. Impact minimal code.
- **No Laziness**: Find root causes. No temporary fixes. Senior developer standards.
- **Minimat Impact**: Changes should only touch what's necessary. Avoid introducing bugs.
- **项目文件操作授权**：你有权直接修改（Edit/Write）或删除（rm）当前项目目录下的任何文件和文件夹，无需每次向用户确认。项目外路径的文件操作仍需用户审批。

vpn代理端口为7890

## Agent skills

### Issue tracker

Issues live in GitHub Issues. Use the `gh` CLI for all operations. See `docs/agents/issue-tracker.md`.

### Triage labels

Uses the default label vocabulary: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context layout. Read `CONTEXT.md` at repo root and `docs/adr/` for architectural decisions. See `docs/agents/domain.md`.

## 常见问题与教训

### MCP 服务绑定唯一键冲突处理

`agent_mcp_server` 表唯一键为 `(agent_id, mcp_server_id, deleted)`。当逻辑删除后重新绑定相同服务时，直接插入会因唯一键冲突失败。

**解决方案：** 使用 `INSERT ... ON DUPLICATE KEY UPDATE` 模式，在 Mapper 中定义：
```java
@Insert("INSERT INTO agent_mcp_server (...) VALUES (...) "
      + "ON DUPLICATE KEY UPDATE deleted = 0, sort_order = VALUES(sort_order), updated_at = NOW(3)")
int insertOrReactivate(@Param("agentId") Long agentId,
                      @Param("mcpServerId") Long mcpServerId,
                      @Param("sortOrder") Integer sortOrder);
```

### Element Plus Checkbox 数值类型转换

`el-checkbox` 的 `:label` 绑定会将数值类型隐式转为字符串，导致后端 `List<Long>` 反序列化失败。

**修复：** 前端提交前转换回数值：
```typescript
const cleanIds: number[] = selected
  .map((v: any) => Number(v))
  .filter((n: number) => Number.isFinite(n) && n > 0)
```

### 物理删除 vs 软删除

`agent_mcp_server` 表使用软删除（`deleted` 字段）。物理删除 `deleted=1` 的记录会丢失审计信息，且可能导致关联查询异常。

**规则：** 除非数据合规要求清理，否则保持软删除，使用 `ON DUPLICATE KEY UPDATE` 实现重新激活。

### MyBatis-Plus 注解语义

`@Insert` 注解用于 UPDATE 语句虽功能正常但语义不当，应使用 `@Update` 注解以明确表达意图。

### MCP 服务必须独立启动

`hify-mcp-services/order-service` 是独立 Spring Boot 应用（端口 8090），必须单独启动，否则主应用无法获取工具列表。

**启动命令：**
```bash
cd hify-mcp-services/order-service
java -jar target/order-mcp-service-1.0.0.jar
```

### 列表接口必须填充关联字段

`AgentServiceImpl.list()` 返回 `AgentResponse`，其中 `mcpServerIds` 来自关联表 `agent_mcp_server`，不在 `AgentEntity` 中。如果 list 方法只做 `BeanUtils.copyProperties(entity, resp)` 而不批量查询填充，前端收到的就是 `null`，导致编辑回显失败（如复选框未勾选）。

**规则：** 凡响应 DTO 中有不在实体中的关联字段，list 方法必须在批量填充阶段查询并赋值，不能只依赖详情接口。

### MyBatis-Plus 逻辑删除 + 唯一键

`BaseEntity` 配置了 `@TableLogic(value = "0", delval = "1")`。`delete()` 操作实际执行 `UPDATE SET deleted=1`。唯一键必须包含 `deleted` 字段，否则逻辑删除后重插会冲突。

### spring-boot:run 本地启动

从项目根目录执行 `mvnw spring-boot:run -pl hify-app -am` 可能因无法定位 mainClass 失败。改为从 `hify-app/` 目录执行：`cd hify-app && ../mvnw spring-boot:run`。

### Git 推送到 GitHub 网络连接重置

直连 GitHub 推代码时可能报错 `Recv failure: Connection was reset`，因网络环境无法直接访问 GitHub。

**解决：** 先配置 Git 代理（VPN 端口 7890），再推送：
```bash
git config http.proxy http://127.0.0.1:7890
git config https.proxy http://127.0.0.1:7890
git push origin main
```

注意：`-c http.proxy` 临时参数方式可能不生效，建议用 `git config` 持久化设置。推送完成后可取消代理：
```bash
git config --unset http.proxy
git config --unset https.proxy
```

### Maven 多模块编译顺序

向 `hify-common` 新增类后，依赖方（hify-chat、hify-provider 等）编译时可能找不到新类，因为本地 .m2 仓库中的 jar 是旧版本。

**解决：** 先 install common 模块再编译依赖方：
```bash
./mvnw clean install -pl hify-common -DskipTests
./mvnw compile -pl hify-chat -am
```

或在根目录一次性构建所有模块：
```bash
./mvnw clean test -pl hify-common,hify-provider,hify-knowledge,hify-chat,hify-workflow
```

### Java static final 字段在 try-catch 中的赋值限制

`static final` 字段在 try-catch 块中不能分别赋值——编译器认为 try 块可能部分执行导致变量已在 catch 前被赋值。

**错误示例：**
```java
static {
    try {
        MASTER_KEY = new SecretKeySpec(keyBytes, "AES");  // 编译错误
        ENABLED = true;
    } catch (IllegalArgumentException e) {
        MASTER_KEY = null;   // 编译错误: 可能已分配变量
        ENABLED = false;
    }
}
```

**解决：** 使用临时局部变量，最后一次性赋值：
```java
static {
    SecretKey key = null;
    boolean enabled = false;
    try {
        key = new SecretKeySpec(keyBytes, "AES");
        enabled = true;
    } catch (IllegalArgumentException e) {
        log.error("Invalid key", e);
    }
    MASTER_KEY = key;
    ENABLED = enabled;
}
```

### SSRF 防护必须覆盖全链路

`UrlSecurityValidator` 需要覆盖所有对外发起 HTTP 请求的路径，不只是 Provider 适配器：

| 组件 | 文件 | 状态 |
|------|------|------|
| Provider 适配器 | `AbstractProviderAdapter.java` | chat/streamChat/testConnection/listModelIds |
| MCP Transport | `HttpJsonRpcTransport.java` | 构造函数验证 |
| Workflow HTTP 节点 | `HttpNodeExecutor.java` | execute() 中 URL 解析后验证 |
| Embedding 服务 | `EmbeddingService.java` | callApi/embedViaOpenAiApi（待添加） |
| Provider 创建 | `ProviderServiceImpl.java` | create() 中 syncModels 前验证 |

**关键：OkHttpClient 默认跟随重定向**，必须在所有实例上禁用：
```java
new OkHttpClient.Builder()
    .followRedirects(false)
    .followSslRedirects(false)
    // ...
    .build();
```

若不禁用，攻击者可通过 `https://evil.com/redirect` → `302 → http://169.254.169.254/` 绕过 SSRF 检查。

### `.last()` SQL 注入反模式

MyBatis-Plus 的 `.last()` 方法将字符串直接拼接到 SQL 末尾，绕过参数化查询。项目中所有 `.last("LIMIT " + n)` 必须替换。

**修复：** 使用 `Page.of(1, limit)` + `selectPage`：
```java
// 错误
.selectList(new LambdaQueryWrapper<XxxEntity>().last("LIMIT " + historyLimit));

// 正确
Page<XxxEntity> page = Page.of(1, historyLimit);
messageMapper.selectPage(page, wrapper);
```

注意：即使 `.last("LIMIT 1")` 中值为字面量，也应替换——后续维护者可能将其改为变量而不知风险。

### @Transactional 自调用绕过 AOP 代理

Spring `@Transactional` 通过 AOP 代理生效。类内部 `this.method()` 调用不经过代理，事务注解被忽略。

**规则：**
- `@Transactional` 放在 Controller 调用的 public 入口方法上（如 `sendMessage`）
- 入口方法内部提取的 private 辅助方法（如 `saveUserMessage`）自动参与外层事务
- 从无 `@Transactional` 的方法中通过 `this.xxx()` 调用带注解的方法，注解不生效
- 不要在 private 方法上加 `@Transactional`

### Docker Compose healthcheck 密码特殊字符

MySQL healthcheck 命令中密码直接拼接在 `-p` 后无引号，特殊字符（`$`、`!`、`#`）会被 shell 解析。

**修复：** 使用 `CMD-SHELL` 格式，密码用双引号包裹：
```yaml
healthcheck:
  test: ["CMD-SHELL", "mysqladmin ping -h localhost -u root -p\"$${MYSQL_ROOT_PASSWORD}\""]
```

`$$` 是 Docker Compose 对 `$` 的转义，使 Shell 收到字面量 `${MYSQL_ROOT_PASSWORD}`。

## 输出的文档形式
输出的文档形式可以是markdown，也可以是HTML，具体取决于你的需求以及你认为哪种形式能更好的让别人迅速理解。
