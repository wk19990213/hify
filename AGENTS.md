# Hify 项目规范

永远都要记住，你只能对本项目的文件进行操作，不可以操作本项目以外的文件。

## 项目概述

Hify 是简版 AI Agent 开发平台（参考 Dify），可本地部署，面向团队内部小规模使用（20-50 人）。

**技术栈：** Spring Boot 3.x + MyBatis-Plus + MySQL 8.x + Redis 7.x + pgvector，不允许私自改变依赖的版本，如果想要改变必须分析利弊，说出令人信服的理由
**前端：** Vue 3 + TypeScript + Element Plus + Vite

**做什么：** 多模型提供商管理 / Agent 创建与配置 / 对话引擎（流式 SSE）/ RAG 知识库 / 简版工作流编排 / MCP 工具接入 / 管理控制台
**不做什么：** 可视化工作流拖拽 / 多租户权限 / 插件市场 / WebSocket 长连接

### 模块列表

```
hify-app/        # 启动入口
hify-provider/   # 模型提供商
hify-agent/      # Agent 管理
hify-chat/       # 对话引擎
hify-mcp/        # MCP 工具管理
hify-workflow/   # 工作流编排
hify-knowledge/  # 知识库 RAG
hify-common/     # 公共模块
hify-web/        # Vue 前端
deploy/          # Docker + K8s 部署
```

## 核心约束

- 跨模块调用走 Service 接口，不直接引用其他模块的 Mapper 或 Entity
- Controller 只做参数校验和调用 Service
- 公共工具类放 hify-common
- VPN 代理端口 7890
- **项目文件操作授权**：有权直接修改/删除当前项目目录下的任何文件，无需每次确认

## 启动与配置

- 基础设施（MySQL/Redis/Embedding）配置在项目根目录 `.env`（从 `.env.example` 复制并填写实际值）
- `application.yml` 通过 `${MYSQL_PASSWORD:}` 占位符读取环境变量
- **`java -jar` 不会自动读取 `.env`**，需先 `export` 环境变量再启动
- 使用 `restart-backend.sh` 重启后端（自动加载 `.env` 全部变量到环境）
- 使用 `start.sh` 一键启动全部服务（前后端），`stop.sh` 停止
- `start.sh` 的 Redis host 检测有 bug（grep 误匹配 application.yml），手动启动时注意
- 首次部署需手动执行 `schema.sql` 建表：`mysql -h <host> -u root -p<PASS> hify < schema.sql`
- MySQL 8.0 不支持 `ALTER TABLE ADD COLUMN IF NOT EXISTS`，需用 INFORMATION_SCHEMA 检查后动态执行

### 当前环境

- MySQL：VM `192.168.59.128:3306`，库名 `hify`，凭据见 `.env`
- Redis：VM `192.168.59.128:6379`，需密码认证，凭据见 `.env`
- Embedding：硅基流动 API，凭据见 `.env`
- 不依赖本地 Docker，VM 已提供 MySQL + Redis

### 已知编译陷阱

- 死代码清理后可能遗留缺失 import（如 `KnowledgeServiceImpl` 缺少 `ProviderMapper`/`ProviderModelMapper` 导入），编译时检查
- 跨模块依赖需先在依赖模块执行 `mvn install -DskipTests`，否则找不到类

## 编码速查

- RESTful：`/api/v1/{资源复数名}`，非 CRUD 用动词后缀
- 表名/字段名：小写下划线。主键：id bigint 自增。逻辑删除：deleted tinyint
- MyBatis-Plus `@TableLogic` 自动处理 deleted=0，`.last()` 禁用
- 线程池必须自定义参数，禁止 `Executors`
- 日志用 SLF4J 占位符，禁止字符串拼接；异常日志必须带栈；敏感信息脱敏

## 前端陷阱

- **axios 拦截器已解包一层**：`request.ts` 响应拦截器在 `code=200` 时 `return data`（即 `response.data.data`），调用方直接 `res.xxx`，不要 `res.data.xxx`
- **SSE 流式请求走 `fetch()` 不走 axios**：需手动从 `localStorage` 取 `hify_token` 加入 `Authorization` header，并检查 `response.ok` 处理 401

## 工作原则

- **Plan First** — 非 trivial 任务先写计划
- **Skill First** — 当用户指令关键词匹配可用 Skill 时，必须调用 `Skill` 工具而非手动执行。尤其 TDD/测试/重构类任务，手动执行会偷懒跳过 RED 阶段，Skill 强制完整流程
- **Simplicity First** — 最小改动，不引入不必要抽象
- **Surgical Changes** — 只触碰必要的代码，匹配现有风格
- **Goal-Driven** — 定义验证标准，循环直到通过

> 原则详解 → `docs/principles.md`

### Skill 触发映射

| 关键词 | Skill |
|--------|-------|
| TDD、red-green、测试驱动、写测试、单元测试 | `tdd` 或 `test-driven-development` |
| 重构、拆分大类、提取方法 | `refactor-ops` |
| 代码审查、review | `code-review` 或 `review` |
| 诊断、debug、排查 | `diagnose` 或 `systematic-debugging` |
| 安全、漏洞、XSS、SQL注入 | `security-ops` |
| 性能、优化、慢查询 | `perf-ops` |
| 前端组件、Vue组件 | `vue-ops` |

## 文档索引

| 文档 | 内容 |
|------|------|
| `docs/architecture.md` | 架构、模块依赖、缓存策略、部署、MCP架构、数据模型 |
| `docs/code-standards.md` | 代码组织、分层规则、DB规范、API规范、阿里巴巴编码规约 |
| `docs/security.md` | SSRF/SQL注入/XSS/事务/文件上传/加密安全 |
| `docs/lessons.md` | 常见问题与教训（MCP绑定、类型转换、编译顺序等） |
| `docs/principles.md` | 四大工作原则详解 + Workflow Orchestration |
| `.Codex/techdebt-rules.json` | 死代码扫描跳过规则 |
