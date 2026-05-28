---
name: 集成测试
description: 对模块进行集成测试。先规划测试清单，确认配置基础，从简单场景开始递进，外部 API mock、DB 用 H2。
---

# 集成测试 Skill

## 触发方式

```
/集成测试 <模块名>
```

示例：
```
/集成测试 hify-provider
/集成测试 hify-chat
```

---

## 流程总览

```
规划清单 → 读配置确认基础 → 用户确认 → 从最简单场景开始 → 跑通 → 下一个 → 全部通过
```

**关键原则**：
- 用户确认清单前不写代码
- 一次只写一个测试，跑通后再写下一个
- 已知 bug 先写红测试，再修 bug

---

## 一、读配置确认技术基础

在输出测试清单前，必须先确认以下技术基础：

### 1.1 检查文件

| 检查项 | 文件 | 确认内容 |
|--------|------|----------|
| 测试依赖 | 被测模块 `pom.xml` | `spring-boot-starter-test` 是否存在 |
| H2 依赖 | 被测模块 `pom.xml` | `h2` 是否已添加 |
| 测试 profile | `src/test/resources/application-test.yml` | 是否存在，覆盖了 MySQL/Redis |
| 主配置 | `src/main/resources/application.yml` | 数据源、Redis、外部 API URL |

### 1.2 如果缺基础配置，先补齐再给清单

- **缺 H2 依赖**：在 `hify-app/pom.xml` 添加 `com.h2database:h2`（scope test）
- **缺 application-test.yml**：创建，覆盖数据源为 H2 内存库，关闭 Redis/LLM 外部调用
- **缺测试基类**：创建 `BaseIntegrationTest.java`

### 1.3 标准 application-test.yml 模板

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:
  sql:
    init:
      mode: always
      schema-locations: classpath:db/schema.sql
  redis:
    lettuce:
      enabled: false
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
      - org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration

logging:
  level:
    com.hify: DEBUG
```

### 1.4 标准测试基类模板

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public abstract class BaseIntegrationTest {
    @Autowired
    protected MockMvc mockMvc;
}
```

---

## 二、测试清单规划

### 2.1 基于 AGENTS.md 核心链路地图

从 AGENTS.md 的数据模型和模块依赖关系出发，列出该模块的核心链路。例如 Provider 模块：

```
provider (模型提供商)
  └── model_config (模型配置) [1:N]
```

核心链路：
- Provider CRUD（创建/列表/详情/更新/删除）
- Provider → ModelConfig 1:N 关系
- 连通性测试（外部 API）
- 状态管理（启用/禁用）

### 2.2 输出格式（表格）

| IT 编号 | 场景 | 请求 | 验证点 | 优先级 |
|---------|------|------|--------|--------|
| IT-P01 | 创建 Provider + 查询 | POST → GET /{id} | 返回完整字段，status=1 | P0 |
| IT-P02 | 创建 Provider 缺必填字段 | POST name=null | 400 + 错误码 | P0 |
| IT-P03 | 更新 Provider | PUT /{id} | 字段已更新 | P1 |
| IT-P04 | Provider 列表分页 | GET ?page=1&pageSize=5 | 返回分页结构 | P1 |

### 2.3 优先级定义

| 优先级 | 定义 | 要求 |
|--------|------|------|
| P0 | 核心 CRUD 正向 + 必填校验 | 必须全部通过 |
| P1 | 更新/删除/分页/状态流转 | 应该通过 |
| P2 | 边界条件/并发/级联 | 可选，时间允许就做 |

---

## 三、Mock 策略决策表

| 组件 | 策略 | 实现方式 |
|------|------|----------|
| 数据库 (MySQL) | **H2 内存库** | `application-test.yml` 覆盖数据源 |
| Redis | **禁用** | `spring.redis.lettuce.enabled=false` |
| LLM API（外部 HTTP）| **Mock Bean** | `@MockBean` 替换 ProviderAdapter / ChatServiceImpl |
| MCP 服务（外部 HTTP）| **Mock Bean** | `@MockBean` 替换 McpClientManager |
| CircuitBreaker | **Mock Bean** | `@MockBean` 替换 CircuitBreakerService |
| 内部 Service（非被测模块）| **真调用** | 跨模块走真实 Service（H2 保证隔离） |

**为什么 DB 用 H2 而不用 Mock？**
- MyBatis-Plus Mapper 的 SQL 需要真实 JDBC 连接验证
- LambdaQueryWrapper 的条件拼接逻辑需要真实执行
- H2 MySQL 兼容模式覆盖 95% 的 SQL 语法差异

**为什么外部 API 用 Mock Bean？**
- LLM/MCP 不可控，不可依赖
- 避免测试速度受外部网络影响
- Mock 返回固定响应，验证业务逻辑而非通信协议

---

## 四、测试数据隔离

### 4.1 每个测试类独立数据

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional  // 每个测试方法执行完自动回滚
@Sql(scripts = "/test-data/provider-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ProviderIntegrationTest extends BaseIntegrationTest { ... }
```

- `@Transactional`：自动回滚，不污染后续测试
- `@Sql`：可选，用于复杂的前置数据（关联表等）
- 每个模块的测试数据 SQL 放在 `src/test/resources/test-data/{模块名}/`

### 4.2 数据清理优先级

1. 优先用 `@Transactional` 回滚（轻量，不写磁盘）
2. 跨事务场景（如测试事务提交后的查询）用 `@Sql` 先清后插
3. 禁止测试间共享数据（不依赖执行顺序）

---

## 五、场景递进原则

从最简单开始，逐个跑通：

```
IT-P01: 简单 CRUD 正向 → 写测试 → 跑 → 通过
  ↓
IT-P02: 参数校验 → 写测试 → 跑 → 通过
  ↓
IT-P03: 更新操作 → ...
  ↓
IT-P04: 列表分页 → ...
```

### 规则

- **一次只写一个测试方法**，跑通后再写下一个
- 不批量生成全部测试代码再一起跑
- 如果当前测试失败了：
  1. 判断是测试写错还是实现有 bug
  2. 如果实现有 bug 且不是本次改动引入的 → 标记为已知 bug，走红→绿流程
  3. 如果实现有 bug 且是本次改动引入的 → 立即修复

### 每个测试方法的标准结构

```java
@Test
void shouldCreateProviderAndReturnFullFields() throws Exception {
    // 1. 准备请求体
    String body = """
        {
          "name": "测试提供商",
          "code": "test-provider",
          "type": "openai",
          "baseUrl": "https://api.openai.com/v1",
          "authConfig": {"apiKey": "sk-test"},
          "status": 1
        }
        """;

    // 2. 发送请求
    MvcResult result = mockMvc.perform(post("/api/v1/providers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data.id").isNumber())
        .andExpect(jsonPath("$.data.name").value("测试提供商"))
        .andReturn();

    // 3. 验证 DB 落库（可选）
    // 通过 GET /{id} 二次验证
    int id = JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
    mockMvc.perform(get("/api/v1/providers/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.code").value("test-provider"));
}
```

---

## 六、已知 Bug 的红→绿流程

当测试暴露了已知 bug（非本次改动引入）：

### 6.1 先写红测试

```java
@Test
void shouldNotAllowDuplicateProviderCode() {
    // 先创建一个 provider
    // 再用相同 code 创建第二个 → 期望 409
    // 此测试标记为 @Tag("known-bug") → 当前 FAIL
}
```

### 6.2 再修 bug

修复实现代码，让测试从红变绿。

### 6.3 验证回归

确保修复后已有的其他测试不受影响。

### 6.4 输出格式

```
已知 Bug 清单:
  IT-P07: Provider code 唯一约束未生效 — 已修，测试通过
  IT-P12: 分页 page=0 返回异常而非默认 page=1 — 待修
```

---

## 七、完成输出

测试全部通过后的输出格式：

```
hify-provider 集成测试结果:
  通过: IT-P01~IT-P06 (6/6)
  失败: 0
  Mock 策略: DB=H2, LLM=MockBean, Redis=禁用
  数据隔离: @Transactional 回滚
```

---

## 八、注意事项

- 禁止用 `@SpringBootTest` 加载整个应用后再 mock 一半 Bean——要么全真（用 H2+MockBean 模拟外部），要么全 mock（纯单元测试）
- 禁止在集成测试中使用 `@Mock` / `@InjectMocks`（那是单元测试的）
- `@MockBean` 只能 mock Spring 容器中的 Bean，不能 mock 静态方法或 new 出来的对象
- 如果被测模块依赖其他模块的 Service，不要 mock——走真实调用（H2 保证隔离）
- H2 MySQL 模式不支持 `ON UPDATE CURRENT_TIMESTAMP(3)`，建表 DDL 如有此语法需用 `schema-test.sql` 单独处理
- 测试跑完后检查 H2 控制台不要在生产环境打开
