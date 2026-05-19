# CLAUDE.md

开始新模块前，先看 .claude/skills/ 目录

降低 AI 写代码常见错误的行为准则。可与项目专属规则合并使用。

**权衡：** 这些规则偏向"谨慎"而非"速度"。琐碎任务，请自行判断。

## 1. 动手前先想清楚（Think Before Coding）

**不假设、不藏糊涂、把权衡摊开来说。**

实现之前：
- 把你的假设明确说出来，不确定就问。
- 有多种理解时，把所有可能列出来——别擅自挑一个就闷头干。
- 如果有更简单的方案，说出来，必要时反驳我。
- 哪里不清楚，就停下来，指出哪里让你困惑，然后问我。

## 2. 最小化原则（Simplicity First）

**只写解决问题的最小代码，不要任何"以防万一"。**

- 不写需求里没要求的功能。
- 不为一次性使用的代码做抽象。
- 不加未要求的"灵活性"或"可配置性"。
- 不为不可能发生的场景写 error handling。
- 如果你写了 200 行而 50 行就够，请重写。

自问一句："资深工程师会觉得这写得太复杂了吗？"如果是，请简化。

## 3. 外科手术式改动（Surgical Changes）

**只动该动的，只清自己的烂摊子。**

修改现有代码时：
- 别"顺手改进"周边代码、注释或格式。
- 别重构没坏的东西。
- 配合现有的代码风格，哪怕你不喜欢。
- 看到无关的死代码——告诉我，但别删。

如果你的改动产生了孤儿代码：
- 删掉因你改动而失去用途的 import / 变量 / 函数。
- 不要删原本就存在的死代码，除非我让你删。

判断标准：每一行改动都能追溯到我的需求。

## 4. 目标驱动执行（Goal-Driven Execution）

**定义成功标准，然后循环到通过为止。**

把任务转换成可验证的目标：
- "加个校验" → "写无效输入的测试，然后让它们通过"
- "修这个 bug" → "写能复现这个 bug 的测试，然后让它通过"
- "重构 X" → "保证重构前后测试都通过"

多步任务，给一个简短的计划：

1. [步骤] → 验证：[检查项]
2. [步骤] → 验证：[检查项]
3. [步骤] → 验证：[检查项]

强的成功标准让你能独立闭环；弱的（比如"让它跑起来"）会让你不停回来问我。

---

**这些规则生效的标志：** diff 里不必要的改动变少了，因过度复杂而返工的变少了，澄清问题出现在动手之前而不是出错之后。

## 输出规则

1. 直接给结论，不要前置解释和铺垫
2. 不要复述我的问题，不要说"好的"、"明白了"、"让我来…"这类引导词
3. 区分大小事项，简单问题一句话回答，复杂问题才展开
4. 客观陈述事实和方案，不要"很棒的问题"、"非常聪明"这类捧场
5. 不要在回答末尾加多余总结
6. 不确定就直接停下来问，不要瞎猜

- 不说废话、不捧用户、纯净输出
请记住我是谁、记住我的偏好，写到记忆文件里。
修bug前，先用一句话复述我描述的问题，确认理解对了再动手


## 项目概述

Hify 是一个简版的 AI Agent 开发平台（参考 Dify），可本地部署，面向团队内部小规模使用（20-50 人同时在线）。

### 做什么
- 多模型提供商管理（OpenAI、Claude、Gemini、Ollama）
- Agent 创建与配置（选模型、绑工具、设系统提示词）
- 对话引擎（流式响应、多轮对话、上下文管理）
- 知识库 + RAG（一期只支持 TXT 文档，固定长度分块）
- 简版工作流（JSON 配置，线性 + 条件分支，不做可视化拖拽）
- MCP 工具接入（Agent 可通过 MCP 协议调用外部工具）
- 管理控制台（模型管理、Agent 配置、对话界面）

### 不做什么
- 不做可视化工作流拖拽编排
- 不做多租户 / 权限体系
- 不做插件市场、计费系统
- 不做文本生成应用、WebApp 发布、嵌入组件
- 不做标注与微调

### 技术栈
后端：Spring Boot 3.x + MyBatis-Plus + MySQL 8.x + Redis 7.x
前端：Vue 3 + TypeScript + Vite + Element Plus
容器化：Docker + Docker Compose

### 部署与运维预期
- Docker Compose 本地一键部署，JVM 内存设上限（-Xmx512m）
- 目标：20-50 人同时在线，峰值 3-5 QPS，瓶颈在 LLM 长连接
- 缓存：Redis Cache-Aside（配置信息 + 会话上下文）
- 监控：起步 Actuator + 日志，后期 Prometheus + Grafana

## 架构设计

### 应用架构
模块化单体。一个 Spring Boot 应用，Maven 多模块组织。

模块划分：
hify/
├── hify-app/               # 启动模块，Spring Boot Application
├── hify-provider/           # 模型提供商管理
├── hify-agent/              # Agent 管理与配置
├── hify-chat/               # 对话引擎
├── hify-mcp/                # MCP 工具管理与调用
├── hify-workflow/           # 工作流编排与执行
├── hify-knowledge/          # 知识库与 RAG
├── hify-common/             # 公共模块（工具类、常量、异常、DTO）
├── hify-web/                # Vue 前端
└── deploy/                  # Docker Compose 配置
依赖原则：单向依赖，不循环。共用逻辑下沉 hify-common。


### 代码组织
每个业务模块统一结构：
src/main/java/com/hify/{module}/
├── controller/        # REST 接口
├── service/           # 业务逻辑接口
├── service/impl/      # 业务逻辑实现
├── mapper/            # MyBatis-Plus Mapper
├── entity/            # 数据库实体
├── dto/               # 请求/响应对象
├── config/            # 配置类
├── exception/         # 模块级自定义异常
└── constant/          # 模块级常量

分层规则：
- Controller 只做参数校验和调用 Service，不写业务逻辑
- Service 处理所有业务逻辑，包括事务管理
- 跨模块调用走 Service 接口，不直接引用其他模块的 Mapper 或 Entity
- Entity 不直接返回给前端，用 DTO 做转换


## LLM 调用规范

### 线程池配置

```java
// llm-pool: 非流式调用（阻塞等待完整响应）
@Bean("llmExecutor")
public ThreadPoolExecutor llmExecutor() {
    return new ThreadPoolExecutor(20, 50, 60L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(100),
        new ThreadFactoryBuilder().setNameFormat("llm-pool-%d").setDaemon(true).build(),
        new ThreadPoolExecutor.CallerRunsPolicy()  // 满载时调用方线程执行，不丢任务
    );
}

// llm-stream: 流式 SSE 调用（长连接）
@Bean("llmStreamExecutor")
public ThreadPoolExecutor llmStreamExecutor() {
    return new ThreadPoolExecutor(30, 80, 60L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(50),
        new ThreadFactoryBuilder().setNameFormat("llm-stream-%d").setDaemon(true).build(),
        new AbortPolicy()  // 流式超限直接拒绝，由上层返回 503
    );
}
```\

### OkHttpClient 配置

```java
// 非流式：有 readTimeout
@Bean("standardLlmClient")
public OkHttpClient standardLlmClient() {
    return new OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .connectionPool(new ConnectionPool(20, 5, TimeUnit.MINUTES))
        .addInterceptor(new LoggingInterceptor())
        .build();
}

// 流式：readTimeout 设为 0（SSE 不能有读超时）
@Bean("streamLlmClient")
public OkHttpClient streamLlmClient() {
    return new OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build();
}
```\

### 超时层次（三层保护）

1. OkHttp connectTimeout = 5s（TCP 握手超时）
2. OkHttp readTimeout = 120s（单次读取超时，仅非流式）
3. CompletableFuture.get(90, TimeUnit.SECONDS)（总体超时兜底）

### 重试策略（Resilience4j）

- 普通 LLM：最多 3 次，初始等待 500ms，指数退避 2x，最大等待 10s
- Ollama（本地）：最多 5 次，初始等待 2s
- 仅对网络异常和 5xx 重试，4xx（参数错误）不重试

### 熔断器配置

```yaml
# COUNT_BASED 滑动窗口，20 次请求内失败率 >50% 触发熔断
# 慢调用（>30s）超过 80% 也触发熔断
# 熔断后等待 30s 进入 half-open，放行 5 次探测
failure-rate-threshold: 50
slow-call-duration-threshold: 30s
slow-call-rate-threshold: 80
wait-duration-in-open-state: 30s
permitted-calls-in-half-open-state: 5
```\

### Fallback 路由

```yaml
hify.llm.fallback:
  openai: ollama
  claude: openai
  gemini: ollama
```\

主 Provider 熔断或异常时自动切换 fallback，fallback 失败则抛出 BizException。

---

## 部署架构

用户浏览器
    │
    ▼
Ingress Nginx（L7 负载均衡 + SSL 终止 + SSE 支持）
    │
    ├──▶ hify-frontend（Vue SPA，Nginx 静态文件服务，2 副本）
    │
    └──▶ hify-backend（Spring Boot，2 副本）
              │
              ├──▶ MySQL 8.x（主数据存储）
              ├──▶ Redis（Session / 缓存 / 限流）
              └──▶ PostgreSQL + pgvector（向量存储）

**Ingress 关键配置（SSE 必须）**：

```yaml
nginx.ingress.kubernetes.io/proxy-read-timeout: "300"
nginx.ingress.kubernetes.io/proxy-buffering: "off"
nginx.ingress.kubernetes.io/limit-rps: "20"
```\

**Backend 容器规格**：requests 512Mi/250m，limits 1Gi/1000m，replicas=2

**JVM 启动参数**：

```dockerfile
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+UseG1GC",
            "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
```\

---

## 数据库规范

### MySQL 通用字段约定

每张表必须包含以下字段：

```sql
id          BIGINT          NOT NULL AUTO_INCREMENT,  -- 主键，禁用 UUID
created_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
updated_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
deleted     TINYINT(1)      NOT NULL DEFAULT 0,       -- 逻辑删除标志
PRIMARY KEY (id)
```\

- 字符集：`utf8mb4`，排序规则：`utf8mb4_unicode_ci`
- 禁用 `VARCHAR` 无长度约束，text 类 content 字段用 `MEDIUMTEXT`
- 金额用 `DECIMAL(19,4)`，禁用 `FLOAT/DOUBLE`
- 布尔用 `TINYINT(1)`，不用 `BIT`

### 索引设计原则

1. **区分度低的字段不单独建索引**（如 deleted、status 枚举），必须与高区分度字段组合
2. **组合索引遵循最左前缀**：等值查询字段在左，范围查询字段在右
3. **查询条件中含 `deleted`**，必须将 `deleted` 纳入索引
4. **每表索引不超过 5 个**（含主键），写多读少的表控制在 3 个以内
5. **禁止在 `TEXT/BLOB` 类型字段上建普通索引**，需要时建前缀索引或全文索引

```sql
-- 正确示例：conversation_id 高区分度在左，deleted 次之，created_at 范围在右
INDEX idx_conv_created (conversation_id, deleted, created_at)
```\

### 大表处理策略

判断为大表的阈值：行数 > 500 万 或 数据量 > 2GB

| 场景 | 策略 |
|------|------|
| t_message | 按 conversation_id 分区，或按月归档冷数据 |
| 知识库向量表 | ivfflat 索引，lists = sqrt(行数) |
| 日志类表 | 只保留 90 天，定期 DELETE + OPTIMIZE TABLE |

### 分页查询规范

- **禁止** `LIMIT offset, size` 深分页（offset > 1000 全表扫描）
- 对话记录类使用**游标分页**：

```sql
SELECT id, role, content, created_at FROM t_message
WHERE conversation_id = ?
  AND deleted = 0
  AND (created_at < ? OR (created_at = ? AND id < ?))
ORDER BY created_at DESC, id DESC
LIMIT 20;
```\

- 管理后台必须分页时，用 `WHERE id > lastId LIMIT size` 替代 offset

### pgvector 索引规范

```sql
-- 余弦相似度索引，lists 值 = sqrt(总行数)，行数 <10 万时 lists=100
CREATE INDEX idx_embedding_ivfflat ON knowledge_embedding
USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- 查询时设置 probes，精度和速度平衡
SET ivfflat.probes = 10;
SELECT * FROM knowledge_embedding
ORDER BY embedding <=> '[...]'::vector LIMIT 5;
```\

### 索引检测措施

**开发阶段**：启用 p6spy，拦截执行 >10ms 的查询自动 EXPLAIN，type=ALL 时打印警告日志。

**CI 阶段**：关键查询写 `IndexCoverageTest`，EXPLAIN 结果中 type=ALL 则测试失败，阻断合并。

**生产阶段**：定期查询 `performance_schema.events_statements_summary_by_digest`，找出 `sum_no_index_used > 0` 的 SQL。

```sql
SELECT digest_text, count_star AS 执行次数, sum_no_index_used AS 未用索引次数
FROM performance_schema.events_statements_summary_by_digest
WHERE sum_no_index_used > 0
ORDER BY sum_no_index_used DESC LIMIT 20;
```\

---

## 编码规范（基于阿里巴巴 Java 开发手册）

### 命名

1. **类名用 UpperCamelCase**，方法名、变量名用 lowerCamelCase，常量用 UPPER_SNAKE_CASE，包名全小写无下划线。
2. **禁止用拼音或拼音缩写**命名，禁止单字母变量（循环变量 `i/j/k` 除外）。
3. **方法名体现动词**：查询用 `get/list/query`，修改用 `update`，删除用 `delete/remove`，新增用 `create/add`，布尔返回值用 `is/has/can`。
4. **Service 接口不加 I 前缀**，实现类加 `Impl` 后缀（`AgentService` + `AgentServiceImpl`）。
5. **数据库表名用 `t_` 前缀**，列名用 snake_case；PO 类用 `Po` 后缀，DTO 用 `Dto`/`Request`/`Response`，Mapper 用 `Mapper` 后缀。

### 异常处理

6. **禁止 catch 后 `e.printStackTrace()` 或空 catch**，必须记录日志或向上抛出。
7. **业务异常统一抛 `BizException(ErrorCode)`**，不用 RuntimeException 传递业务语义。
8. **只在顶层（GlobalExceptionHandler）处理并转换为 HTTP 响应**，中间层不捕获再包装。
9. **finally 块不写 return**，不在 finally 中抛出新异常（会吞掉原始异常）。
10. **NPE 防御**：方法返回值优先返回空集合（`Collections.emptyList()`）而非 null，接口入参用 `@NonNull`/`@Valid` 注解声明约束。
11.异常处理必须使用ErrorCode枚举，禁止硬编码错误码和错误信息


### 日志

11. **使用 SLF4J 接口 + Logback 实现**，类中用 `@Slf4j`（Lombok），禁止用 `System.out.println`。
12. **禁止在循环体内打日志**，高频路径只在异常分支记录。
13. **占位符格式 `log.info("xxx {}", var)`**，禁止字符串拼接（避免无效 toString 开销）。
14. **日志分级约定**：DEBUG=详细调试，INFO=关键业务节点，WARN=可恢复异常或配置缺失，ERROR=需人工介入的故障。生产环境 INFO 级别，日志文件按天滚动，保留 30 天。
15. **LLM 调用必须记录**：provider、model、耗时、token 数、是否命中缓存，便于成本分析。

### 前端编码偏好

- API 方法使用 `const` 箭头函数导出，比 `function` 声明更简洁
- 调用 `request` 方法时显式标注泛型 `<T>`，让调用方直接拿到 `Promise<T>` 而非 `Promise<any>`

```ts
// 推荐
export const getHealth = () => get<string>('/v1/health')

// 不推荐
export function getHealth() {
  return get('/v1/health')
}
```

### 并发

16. **线程池必须显式创建**（`ThreadPoolExecutor`），禁止用 `Executors.newFixedThreadPool`（无界队列 OOM）。
17. **ThreadLocal 用完必须 `remove()`**，防止线程池场景下数据泄漏。
18. **加锁粒度最小化**：只锁共享变量操作，不锁 I/O 和 LLM 调用；优先用 `ReentrantLock` 替代 `synchronized`（可设超时）。
19. **单例 Bean 的成员变量必须是线程安全的**：无状态 Service 天然安全；有状态则用 `ThreadLocal` 或局部变量，禁止用实例变量存请求上下文。
20. **`CompletableFuture` 异步调用必须指定线程池**（`supplyAsync(task, llmExecutor)`），禁止用默认 `ForkJoinPool.commonPool()`（会影响其他异步任务）。

---

## 性能瓶颈优先级（一期处理清单）

| 级别 | 瓶颈 | 一期处理方式 |
|------|------|-------------|
| P0 | LLM API 延迟高（3-30s） | 线程隔离 + 熔断 + Fallback（已设计） |
| P0 | 向量检索无索引全表扫描 | 建 ivfflat 索引（建表时必须创建） |
| P1 | 对话消息深分页 | 游标分页（禁止 LIMIT offset） |
| P1 | N+1 查询 | MyBatis-Plus 批量查询，禁止循环单查 |
| P2 | 连接池耗尽 | HikariCP 配置：maximumPoolSize=20，connectionTimeout=3000ms |
| 延后 | 静态资源未压缩 | Nginx gzip，流量大时处理 |
| 延后 | JVM GC 停顿 | G1GC 已启用，暂不调优 |