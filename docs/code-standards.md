# Hify 代码规范

## 后端模块内部结构

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

## 分层规则

- Controller 只做参数校验和调用 Service，不写业务逻辑
- Service 处理所有业务逻辑
- 跨模块调用走 Service 接口，不直接引用其他模块的 Mapper 或 Entity
- 公共工具类、基类放 hify-common

## 前端结构

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

## 接口规范

RESTful 风格：`/api/v1/{资源复数名}`
```
GET    /api/v1/providers              # 列表（分页）
POST   /api/v1/providers              # 创建
GET    /api/v1/providers/{id}         # 详情
PUT    /api/v1/providers/{id}         # 更新
DELETE /api/v1/providers/{id}         # 删除
POST   /api/v1/providers/{id}/test-connection  # 非 CRUD 操作用动词
```

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
- MyBatis-Plus `@TableLogic` 自动处理 `deleted=0` 过滤，无需手动添加 `.eq(deleted, 0)`

## 核心数据模型

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

## Spring Boot/Java 编码规范（阿里巴巴手册精简版）

### 命名规范

- **类名**：大驼峰，名词。正例：`ChatMessageService`。反例：`chatMessageService`
- **方法/变量**：小驼峰，动词开头（方法），名词（变量）。正例：`getUserById()`, `maxCount`
- **常量**：全大写+下划线。正例：`MAX_RETRY_TIMES`
- **包名**：全小写，点分隔，禁止复数。正例：`com.hify.chat.service`
- **布尔变量**：禁用 is 开头（避免序列化问题）。正例：`deleted`, `enabled`

### 异常处理

- 禁止吞异常：catch 必须处理，禁止空 catch 块
- 禁止用异常做流程控制
- 自定义异常必须继承 RuntimeException
- 跨层抛异常：Service 向上抛自定义异常，Controller 统一捕获
- NPE 防范：返回空集合用 `Collections.emptyList()`，禁止返回 null

### 日志规范

- 占位符禁止拼接：`log.info("用户登录: userId={}", userId)`
- 禁止 System.out/err：统一用 SLF4J
- 敏感信息脱敏：密码、token、手机号
- 异常日志必须带栈：`log.error("xx失败", e)`，禁止 `e.getMessage()`

### 并发处理

- 线程池必须自定义参数：禁止 `Executors.newFixedThreadPool()`
- `SimpleDateFormat` 禁止 static：用 `DateTimeFormatter`
- 并发修改加锁：用 `ConcurrentHashMap`
- `volatile` 不保证原子性：计数用 `AtomicLong`
