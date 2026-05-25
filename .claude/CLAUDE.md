# Hify 项目规范

永远都要记住，你只能对本项目的文件进行操作，不可以操作本项目以外的文件。

## 项目概述

Hify 是简版 AI Agent 开发平台（参考 Dify），可本地部署，面向团队内部小规模使用（20-50 人）。

**技术栈：** Spring Boot 3.x + MyBatis-Plus + MySQL 8.x + Redis 7.x + pgvector
**前端：** Vue 3 + TypeScript + Element Plus + Vite
**容器化：** Docker + K8s

**做什么：** 多模型提供商管理 / Agent 创建与配置 / 对话引擎（流式 SSE）/ RAG 知识库 / 简版工作流编排 / MCP 工具接入 / 管理控制台
**不做什么：** 可视化工作流拖拽 / 多租户权限体系 / 插件市场计费系统 / WebSocket 长连接

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

> 架构详情、模块依赖关系、部署策略 → `docs/architecture.md`
> 代码组织、分层规则、数据库规范、接口规范 → `docs/code-standards.md`
> 安全规范（SSRF/SQL注入/XSS/事务/文件上传）→ `docs/security.md`
> 常见问题与教训 → `docs/lessons.md`

## 核心约束

- 跨模块调用走 Service 接口，不直接引用其他模块的 Mapper 或 Entity
- Controller 只做参数校验和调用 Service
- 公共工具类放 hify-common
- `.env` 不进 Git（模板用 `.env.example`）
- VPN 代理端口 7890
- **项目文件操作授权**：有权直接修改/删除当前项目目录下的任何文件，无需每次确认

## 编码速查

- 表名/字段名：小写下划线。主键：id bigint 自增。逻辑删除：deleted tinyint
- RESTful：`/api/v1/{资源复数名}`，非 CRUD 用动词后缀
- MyBatis-Plus `@TableLogic` 自动处理 deleted=0，`.last()` 禁用
- 线程池必须自定义参数，禁止 `Executors`
- 日志用 SLF4J 占位符，禁止字符串拼接
- 异常日志必须带栈，敏感信息脱敏

## 原则

- **Plan First**: 非 trivial 任务先写计划
- **Subagent Strategy**: 用子代理隔离上下文
- **Verification Before Done**: 测试通过才算完成
- **Simplicity First**: 最小改动，不引入不必要抽象
- **No Laziness**: 找根因，不临时修复
- 输出文档形式可以是 markdown 或 HTML
