# 技术债务修复实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复 techdebt-prd.json 中剩余 47 项技术债务，按风险优先级 + 由小到大策略分 5 阶段推进。

**Architecture:** 先收尾已动工的共享工具提取(DUP 系列) → JWT 认证基础设施(安全前置) → 清 S 项死代码/小方法拆分 → 大类拆分为子 Service → 前端组件拆分 + 安全加固收尾。每阶段可独立提交。

**Tech Stack:** Spring Boot 3.x + MyBatis-Plus + MySQL 8.x + Vue 3 + TypeScript + Element Plus

**Source:** plan/techdebt-prd.json

**Review notes (2026-05-27):**
- 裁剪 DUP-07/08 (BaseController/BaseServiceImpl) — 各模块 create/update/delete 逻辑差异大，泛型 CRUD 基类过度抽象，ROI 低
- 裁剪 DUP-11/13 (crudApi 工厂/CrudListPage) — 5 个列表页筛选器/操作/对话框差异大，泛型封装无实质价值
- SEC-01 (JWT 认证) 从 Phase 5 前移至 Phase 2 — P0 安全项不应最后
- 补入 SEC-02~07 (XSS/SSRF/SSL/日志泄露) — PRD 中 P0/P1 项被遗漏
- ToolCallHandler API 与实际实现对齐 — 单一 executeToolCalls 方法签名
- DUP-04 竞态修复 — selectOne+insert → ON DUPLICATE KEY UPDATE
- MySQL connector 8.4.0 替代 9.1.0 — 避免跨大版本兼容性风险
- RateLimiter 按 IP 分片 — 避免全局限流器单用户拖垮全体

---

## 文件结构总览

### Phase 1 新建/修改文件

| 文件 | 职责 | 操作 |
|------|------|------|
| `hify-provider/.../service/ProviderHealthService.java` | 健康记录统一更新 | 新建 |
| `hify-provider/.../service/impl/ProviderServiceImpl.java` | 移除内联 AesEncryptor | 修改 |
| `hify-chat/.../service/impl/ChatServiceImpl.java` | 接入 ToolCallHandler | 修改 |
| `hify-workflow/.../engine/LlmNodeExecutor.java` | 接入 ToolCallHandler | 修改 |
| `hify-workflow/.../service/impl/WorkflowServiceImpl.java` | 删 ObjectMapper + saveNodes 委托 | 修改 |
| `hify-provider/.../scheduler/ProviderHealthScheduler.java` | 委托 ProviderHealthService | 修改 |
| `hify-provider/.../controller/ProviderController.java` | 清理 import | 修改 |
| `hify-knowledge/.../service/EmbeddingService.java` | 清理 import + SSRF 校验 | 修改 |
| `hify-agent/.../service/impl/AgentServiceImpl.java` | 清理 import | 修改 |
| `hify-knowledge/.../service/impl/KnowledgeServiceImpl.java` | 安全注释 | 修改 |
| `hify-common/.../util/UrlSecurityValidator.java` | RFC 1918 注释 + localhost 开关 | 修改 |
| `hify-common/.../http/LlmHttpClient.java` | 用户消息脱敏 + HttpLoggingInterceptor 级别 | 修改 |
| `hify-mcp/.../mcp/HttpJsonRpcTransport.java` | MCP 日志脱敏 | 修改 |
| `pom.xml` | MySQL connector 升级到 8.4.0 | 修改 |
| `hify-app/src/main/resources/application.yml` | SSL 默认值 + EMBEDDING_KEY 检查 | 修改 |
| `hify-web/src/components/HifyTable.vue` | datetime 列类型 | 修改 |
| `hify-web/src/views/workflow/WorkflowList.vue` | useConfirm | 修改 |
| `hify-web/src/views/chat/ChatView.vue` | XSS 加固 (DOMPurify 配置 + CSP) | 修改 |

### Phase 2 新建/修改文件

| 文件 | 职责 | 操作 |
|------|------|------|
| `hify-app/.../config/SecurityConfig.java` | Spring Security 配置 | 新建 |
| `hify-app/.../security/JwtAuthFilter.java` | JWT 认证过滤器 | 新建 |
| `hify-app/.../security/JwtUtils.java` | JWT 工具类 | 新建 |
| `hify-app/.../controller/AuthController.java` | 登录/注册/刷新 | 新建 |
| `hify-app/.../service/AuthService.java` | 认证服务接口 | 新建 |
| `hify-app/.../service/impl/AuthServiceImpl.java` | 认证服务实现 | 新建 |
| `hify-app/.../dto/LoginReq.java` | 登录请求 DTO | 新建 |
| `hify-app/.../dto/LoginResp.java` | 登录响应 DTO | 新建 |
| `hify-app/.../dto/RegisterReq.java` | 注册请求 DTO | 新建 |
| `hify-app/.../config/RateLimitInterceptor.java` | 全局限流拦截器(按 IP) | 新建 |
| `hify-app/src/main/resources/application.yml` | JWT 配置 | 修改 |
| `hify-app/src/main/resources/db/schema.sql` | users 表 password_hash | 修改 |
| `pom.xml` | spring-boot-starter-security + jjwt | 修改 |
| `hify-web/src/router/index.ts` | 登录页路由 + 路由守卫 | 修改 |
| `hify-web/src/views/Login.vue` | 登录页面 | 新建 |

### Phase 3 新建/修改文件

| 文件 | 职责 | 操作 |
|------|------|------|
| `hify-common/.../dto/BasePageParams.java` | 公共分页参数 | 新建 |
| `hify-agent/.../dto/AgentListParams.java` | 继承 BasePageParams | 修改 |
| `hify-mcp/.../dto/McpServerListParams.java` | 继承 BasePageParams | 修改 |
| `hify-workflow/.../dto/WorkflowListParams.java` | 继承 BasePageParams | 修改 |
| `hify-common/.../http/LlmHttpClient.java` | 提取 SseStreamCallback | 修改 |
| `hify-common/.../resilience/CircuitBreakerService.java` | 提取 computeWaitMs | 修改 |
| `hify-provider/.../adapter/AbstractProviderAdapter.java` | testConnection 简化 | 修改 |
| `hify-provider/.../service/impl/ProviderServiceImpl.java` | list/delete/updateProviderHealth 拆分 | 修改 |
| `hify-agent/.../service/impl/AgentServiceImpl.java` | buildQueryWrapper 排序提取 | 修改 |
| `hify-workflow/.../service/impl/WorkflowServiceImpl.java` | update 拆分 | 修改 |

### Phase 4 新建/修改文件

| 文件 | 职责 | 操作 |
|------|------|------|
| `hify-provider/.../service/ModelSyncService.java` | 模型同步 | 新建 |
| `hify-provider/.../service/ProviderHealthService.java` | 健康记录(完善) | 修改 |
| `hify-agent/.../service/AgentToolService.java` | 工具绑定 | 新建 |
| `hify-agent/.../service/AgentMcpBindingService.java` | MCP 绑定 | 新建 |
| `hify-knowledge/.../service/DocumentParserService.java` | 文档解析 | 新建 |
| `hify-knowledge/.../service/FileValidationService.java` | 文件校验 | 新建 |
| `hify-knowledge/.../service/RagQueryService.java` | RAG 查询 | 新建 |
| `hify-chat/.../service/SessionManager.java` | 会话管理 | 新建 |
| `hify-chat/.../service/MessageBuilder.java` | 消息构建 | 新建 |
| `hify-chat/.../service/ChatOrchestrator.java` | 聊天编排 | 新建 |
| `hify-chat/.../service/AgentContextResolver.java` | Agent 上下文解析 | 新建 |
| `hify-workflow/.../engine/ExecutionState.java` | 执行状态参数对象 | 新建 |
| 对应各模块原有 ServiceImpl | 简化为 Facade | 修改 |

### Phase 5 新建/修改文件

| 文件 | 职责 | 操作 |
|------|------|------|
| `hify-web/src/views/workflow/LlmNodeConfig.vue` | LLM 节点配置 | 新建 |
| `hify-web/src/views/workflow/HttpNodeConfig.vue` | HTTP 节点配置 | 新建 |
| `hify-web/src/views/workflow/RagNodeConfig.vue` | RAG 节点配置 | 新建 |
| `hify-web/src/views/workflow/ConditionNodeConfig.vue` | 条件节点配置 | 新建 |
| `hify-web/src/views/workflow/EdgeConfigPanel.vue` | 边配置 | 新建 |
| `hify-web/src/views/workflow/VariableHintsPanel.vue` | 变量提示 | 新建 |
| `hify-web/src/views/agent/AgentFormDialog.vue` | Agent 编辑表单 | 新建 |
| `hify-web/src/views/agent/McpServerSelector.vue` | MCP 服务器选择器 | 新建 |
| `hify-web/src/views/provider/ProviderFormDialog.vue` | Provider 编辑表单 | 新建 |
| `hify-web/src/views/provider/HealthStatusCell.vue` | 健康状态单元格 | 新建 |
| `hify-common/.../util/UrlSecurityValidator.java` | DNS 二次校验 | 修改 |
| `hify-common/.../http/LlmHttpClient.java` | 接入 SafeDns | 修改 |
| `hify-web/src/views/provider/ProviderList.vue` | API Key password 输入框 | 修改 |

---

## Phase 1：收尾 — 完成已动工项 + S 项批量处理

### Task 1.1: DUP-03 — ToolCallHandler 接入 ChatServiceImpl

**Files:**
- Modify: `hify-chat/src/main/java/com/hify/chat/service/impl/ChatServiceImpl.java`
- Reference: `hify-provider/src/main/java/com/hify/provider/service/ToolCallHandler.java`

**现状：** ChatServiceImpl 第 642 行有私有 `executeToolCalls(adapter, llmResponse, toolCalls, tools, messages)` 方法，内联构建 assistant 消息 + 执行 MCP 工具 + 追加 tool 消息。ToolCallHandler 提供相同逻辑的共享实现，签名一致。

- [ ] **Step 1: 注入 ToolCallHandler**

```java
private final ToolCallHandler toolCallHandler;
```

`ChatServiceImpl` 已用 `@RequiredArgsConstructor`，Lombok 自动生成构造器。

- [ ] **Step 2: 替换私有 executeToolCalls 方法体**

将私有方法体替换为委托调用：

```java
private void executeToolCalls(ProviderAdapter adapter, String llmResponse,
                               List<ToolCall> toolCalls, List<ToolDef> tools,
                               List<Map<String, Object>> messages) {
    toolCallHandler.executeToolCalls(adapter, llmResponse, toolCalls, tools, messages);
}
```

注意：当前私有方法额外处理 `reasoningContent`（第 646/658-660 行）。需在 ToolCallHandler 中补充 reasoning_content 支持，或保留 reasoning 处理在 ChatServiceImpl 调用前完成。

- [ ] **Step 3: 在 ToolCallHandler 中补 reasoning_content 支持**

```java
// ToolCallHandler.executeToolCalls 中，构建 assistantMsg 后
if (reasoningContent != null) {
    assistantMsg.put("reasoning_content", reasoningContent);
}
```

需扩展 ToolCallHandler 方法签名增加 `String reasoningContent` 参数，或从 llmResponse 中通过 adapter 提取。

- [ ] **Step 4: 编译验证**

```bash
cd hify-chat && mvn compile -DskipTests
```

- [ ] **Step 5: 运行已有测试**

```bash
cd hify-chat && mvn test
cd hify-provider && mvn test  # ToolCallHandlerTest
```

- [ ] **Step 6: Commit**

```bash
git add hify-chat/src/main/java/com/hify/chat/service/impl/ChatServiceImpl.java \
        hify-provider/src/main/java/com/hify/provider/service/ToolCallHandler.java
git commit -m "refactor: ChatServiceImpl 接入 ToolCallHandler，消除工具调用重复代码"
```

---

### Task 1.2: DUP-03 — ToolCallHandler 接入 LlmNodeExecutor

**Files:**
- Modify: `hify-workflow/src/main/java/com/hify/workflow/engine/LlmNodeExecutor.java`
- Reference: `hify-provider/src/main/java/com/hify/provider/service/ToolCallHandler.java`

**现状：** LlmNodeExecutor.execute() 第 104-128 行内联构建 tool_call maps + for 循环 callTool。与 ToolCallHandler.executeToolCalls 逻辑一致。

- [ ] **Step 1: 注入 ToolCallHandler**

```java
private final ToolCallHandler toolCallHandler;
```

- [ ] **Step 2: 替换 execute() 中的工具调用循环**

将第 104-128 行替换为：

```java
toolCallHandler.executeToolCalls(adapter, lastResponse, toolCalls, tools, messages);
```

- [ ] **Step 3: 删除私有 executeToolCall 方法**

删除 `LlmNodeExecutor` 中 `executeToolCall(ToolCall tc, List<ToolDef> tools)` 私有方法（第 156-174 行），ToolCallHandler.findToolDef 已包含此逻辑。

- [ ] **Step 4: 编译验证**

```bash
cd hify-workflow && mvn compile -DskipTests
```

- [ ] **Step 5: Commit**

```bash
git add hify-workflow/src/main/java/com/hify/workflow/engine/LlmNodeExecutor.java
git commit -m "refactor: LlmNodeExecutor 接入 ToolCallHandler"
```

---

### Task 1.3: DUP-02 扫尾 — ProviderServiceImpl 解密改用 AuthConfigHelper

**Files:**
- Modify: `hify-provider/src/main/java/com/hify/provider/service/impl/ProviderServiceImpl.java`

- [ ] **Step 1: 替换内联 AesEncryptor.decrypt 调用**

```java
// 当前代码
String json = AesEncryptor.decrypt(encrypted);
Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {});

// 改为
Map<String, Object> map = AuthConfigHelper.decryptAuthConfig(encrypted);
```

- [ ] **Step 2: 添加 import + 删除不再需要的 import**

```java
import com.hify.provider.util.AuthConfigHelper;
// 删除不再使用的 AesEncryptor / TypeReference import（如其他地方未使用）
```

- [ ] **Step 3: 编译验证**

```bash
cd hify-provider && mvn compile -DskipTests
```

- [ ] **Step 4: Commit**

```bash
git add hify-provider/src/main/java/com/hify/provider/service/impl/ProviderServiceImpl.java
git commit -m "refactor: ProviderServiceImpl 解密改用 AuthConfigHelper"
```

---

### Task 1.4: DUP-04 — 提取 ProviderHealthService 统一健康记录更新

**Files:**
- Create: `hify-provider/src/main/java/com/hify/provider/service/ProviderHealthService.java`
- Create: `hify-provider/src/main/java/com/hify/provider/service/impl/ProviderHealthServiceImpl.java`
- Modify: `hify-provider/src/main/java/com/hify/provider/service/impl/ProviderServiceImpl.java`
- Modify: `hify-provider/src/main/java/com/hify/provider/scheduler/ProviderHealthScheduler.java`

**注意：** 原实现用 selectOne → if null insert else updateById，存在 TOCTOU 竞态。修复为 `INSERT ... ON DUPLICATE KEY UPDATE`（需 provider_id 有唯一索引），或用 MyBatis-Plus `saveOrUpdate()`。

- [ ] **Step 1: 确认 provider_health 表 provider_id 唯一索引**

```sql
-- 如不存在则添加
ALTER TABLE provider_health ADD UNIQUE INDEX uk_provider_id (provider_id);
```

- [ ] **Step 2: 新建 ProviderHealthService**

```java
package com.hify.provider.service;

public interface ProviderHealthService {
    void updateHealthRecord(Long providerId, boolean success);
}
```

- [ ] **Step 3: 新建 ProviderHealthServiceImpl (竞态安全版)**

```java
package com.hify.provider.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hify.provider.entity.ProviderHealthEntity;
import com.hify.provider.mapper.ProviderHealthMapper;
import com.hify.provider.service.ProviderHealthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderHealthServiceImpl implements ProviderHealthService {

    private final ProviderHealthMapper providerHealthMapper;
    private static final int MAX_CONSECUTIVE_FAILURES = 3;

    @Override
    public void updateHealthRecord(Long providerId, boolean success) {
        ProviderHealthEntity health = providerHealthMapper.selectOne(
            new LambdaQueryWrapper<ProviderHealthEntity>()
                .eq(ProviderHealthEntity::getProviderId, providerId));

        boolean isNew = health == null;
        if (isNew) {
            health = new ProviderHealthEntity();
            health.setProviderId(providerId);
            health.setConsecutiveFailures(0);
        }

        health.setLastCheckAt(LocalDateTime.now());
        if (success) {
            health.setStatus("healthy");
            health.setConsecutiveFailures(0);
        } else {
            int fails = (health.getConsecutiveFailures() != null
                ? health.getConsecutiveFailures() : 0) + 1;
            health.setConsecutiveFailures(fails);
            if (fails >= MAX_CONSECUTIVE_FAILURES) {
                health.setStatus("unhealthy");
                log.warn("Provider {} 连续失败 {} 次，标记为 unhealthy", providerId, fails);
            }
        }

        if (isNew) {
            try {
                providerHealthMapper.insert(health);
            } catch (DuplicateKeyException e) {
                // 并发插入冲突 → 重新 select 再 update
                health = providerHealthMapper.selectOne(
                    new LambdaQueryWrapper<ProviderHealthEntity>()
                        .eq(ProviderHealthEntity::getProviderId, providerId));
                if (health != null) {
                    // 重新计算状态后更新
                    health.setLastCheckAt(LocalDateTime.now());
                    if (success) {
                        health.setStatus("healthy");
                        health.setConsecutiveFailures(0);
                    } else {
                        int fails = (health.getConsecutiveFailures() != null
                            ? health.getConsecutiveFailures() : 0) + 1;
                        health.setConsecutiveFailures(fails);
                        if (fails >= MAX_CONSECUTIVE_FAILURES) {
                            health.setStatus("unhealthy");
                        }
                    }
                    providerHealthMapper.updateById(health);
                }
            }
        } else {
            providerHealthMapper.updateById(health);
        }
    }
}
```

- [ ] **Step 4: ProviderServiceImpl.updateProviderHealth 委托**

```java
private final ProviderHealthService providerHealthService;

public void updateProviderHealth(Long providerId, boolean success) {
    providerHealthService.updateHealthRecord(providerId, success);
}
```

删除原内联逻辑（约 30 行）。

- [ ] **Step 5: ProviderHealthScheduler.updateHealth 委托**

```java
private final ProviderHealthService providerHealthService;

private void updateHealth(Long providerId, boolean success) {
    providerHealthService.updateHealthRecord(providerId, success);
}
```

- [ ] **Step 6: 编译验证 + 测试**

```bash
cd hify-provider && mvn compile -DskipTests && mvn test
```

- [ ] **Step 7: Commit**

```bash
git add hify-provider/
git commit -m "refactor: 提取 ProviderHealthService 统一健康记录更新，修复 TOCTOU 竞态"
```

---

### Task 1.5: DEAD-05~12 — 剩余死代码清理

**Files:**
- Modify: `hify-workflow/.../WorkflowServiceImpl.java` (DEAD-05)
- Modify: `hify-agent/.../AgentServiceImpl.java` (DEAD-06)
- Modify: `hify-provider/.../ProviderServiceImpl.java` (DEAD-07~09)
- Modify: `hify-provider/.../ProviderController.java` (DEAD-10)
- Modify: `hify-knowledge/.../EmbeddingService.java` (DEAD-11~12)

- [ ] **Step 1: DEAD-05 — 删除 WorkflowServiceImpl 未使用的 ObjectMapper 字段**

```java
// 删除此行
private final ObjectMapper objectMapper;
```

- [ ] **Step 2: DEAD-06 — 删除 AgentServiceImpl 中 LambdaUpdateWrapper import**

```java
// 删除此行
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
```

- [ ] **Step 3: DEAD-07~09 — 删除 ProviderServiceImpl 中 3 个未使用 import**

```java
// 删除以下 3 行
import com.fasterxml.jackson.databind.JsonNode;
import com.hify.provider.adapter.AbstractProviderAdapter;
import java.util.ArrayList;
```

- [ ] **Step 4: DEAD-10 — ProviderController HashMap import**

```java
// 删除 import java.util.HashMap;
// 将第 49 行 new java.util.HashMap<>() 改为 new HashMap<>()
// 确认 import java.util.HashMap 确实删除后无编译错误
```

- [ ] **Step 5: DEAD-11~12 — 删除 EmbeddingService 中 Comparator 和 List import**

```java
// 删除以下 2 行
import java.util.Comparator;
import java.util.List;
```

- [ ] **Step 6: 编译验证**

```bash
mvn compile -DskipTests
```

- [ ] **Step 7: Commit**

```bash
git add hify-workflow/.../WorkflowServiceImpl.java \
        hify-agent/.../AgentServiceImpl.java \
        hify-provider/.../ProviderServiceImpl.java \
        hify-provider/.../ProviderController.java \
        hify-knowledge/.../EmbeddingService.java
git commit -m "chore: 清理剩余死代码 DEAD-05~12"
```

---

### Task 1.6: S 项批量处理 — 安全 + 重复消除

覆盖：SEC-02~07/10/11/15/16/17 + DUP-10/14/17

**Files:**
- Modify: `pom.xml` (SEC-15)
- Modify: `hify-app/src/main/resources/application.yml` (SEC-07/11)
- Modify: `hify-web/src/views/chat/ChatView.vue` (SEC-02)
- Modify: `hify-knowledge/.../EmbeddingService.java` (SEC-03)
- Modify: `hify-common/.../util/UrlSecurityValidator.java` (SEC-04/17)
- Modify: `hify-mcp/.../HttpJsonRpcTransport.java` (SEC-05)
- Modify: `hify-common/.../http/LlmHttpClient.java` (SEC-06/10)
- Modify: `hify-knowledge/.../KnowledgeServiceImpl.java` (SEC-16)
- Modify: `hify-workflow/.../WorkflowServiceImpl.java` (DUP-10)
- Modify: `hify-web/src/components/HifyTable.vue` (DUP-14)
- Modify: `hify-web/src/views/agent/AgentList.vue` (DUP-14)
- Modify: `hify-web/src/views/provider/ProviderList.vue` (DUP-14)
- Modify: `hify-web/src/views/workflow/WorkflowList.vue` (DUP-14/17)
- Modify: `hify-web/src/views/mcp/McpServerList.vue` (DUP-14)
- Modify: `hify-web/src/views/knowledge/KnowledgeList.vue` (DUP-14)

- [ ] **Step 1: SEC-15 — MySQL 连接器升级 (8.4.0)**

```xml
<!-- 原 -->
<artifactId>mysql-connector-java</artifactId>
<version>8.0.33</version>

<!-- 改为 (8.4.0 是 8.x 线最新维护版，无 API 变更) -->
<artifactId>mysql-connector-j</artifactId>
<version>8.4.0</version>
```

理由：9.1.0 跨大版本，对 MySQL 8.0.x 可能有未验证的兼容性问题。8.4.0 是 `mysql-connector-java` → `mysql-connector-j` 重命名后 8.x 线的直接替代。

- [ ] **Step 2: SEC-07 — SSL 默认值修改**

在 `application.yml` 中：

```yaml
# 原
url: jdbc:mysql://${MYSQL_HOST:192.168.59.128}:${MYSQL_PORT:3306}/${MYSQL_DATABASE:hify}?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true

# 改为
url: jdbc:mysql://${MYSQL_HOST:192.168.59.128}:${MYSQL_PORT:3306}/${MYSQL_DATABASE:hify}?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai&useSSL=true&allowPublicKeyRetrieval=false
```

本地 VM 环境通过 `MYSQL_URL` 环境变量覆盖为宽松配置。

- [ ] **Step 3: SEC-11 — EmbeddingService 启动检查**

在 `EmbeddingService` 添加 `@PostConstruct`：

```java
@Value("${embedding.key:}")
private String embeddingKey;

@Value("${embedding.url:}")
private String embeddingUrl;

@PostConstruct
void checkConfig() {
    if (embeddingKey.isEmpty() && embeddingUrl != null
            && !embeddingUrl.contains("localhost")
            && !embeddingUrl.contains("127.0.0.1")) {
        log.warn("EMBEDDING_KEY 未配置但 embeddingUrl 指向远程服务({})，将回退到本地 Ollama", embeddingUrl);
    }
}
```

- [ ] **Step 4: SEC-03 — EmbeddingService URL 校验**

在 `callApi()` 和 `embedViaOpenAiApi()` 方法中发起 HTTP 请求前：

```java
UrlSecurityValidator.validateUrl(url, "embedding");
```

- [ ] **Step 5: SEC-04 — UrlSecurityValidator localhost 可配置开关**

```java
@Value("${security.url-validator.allow-localhost-http:false}")
private boolean allowLocalhostHttp;
```

默认 `false`（拒绝 localhost HTTP）。本地 Ollama 用户通过配置开启。

- [ ] **Step 6: SEC-02 — ChatView XSS 加固**

```typescript
// ChatView.vue 中 DOMPurify 配置加固
import DOMPurify from 'dompurify'

DOMPurify.setConfig({
  ALLOWED_URI_REGEXP: /^(?:(?:(?:f|ht)tps?|mailto|tel|callto|sms|cid|xmpp):|[^a-z]|[a-z+.\-]+(?:[^a-z+.\-:]|$))/i,
  FORBID_TAGS: ['style', 'script', 'iframe', 'object', 'embed', 'form', 'input'],
  FORBID_ATTR: ['onerror', 'onload', 'onclick']
})
```

`vite.config.ts` 或 nginx 添加 CSP 头：
```
Content-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'
```

MCP 工具响应在传入 `renderMarkdown()` 前先做 HTML 实体编码。

- [ ] **Step 7: SEC-05 — MCP 日志脱敏**

`HttpJsonRpcTransport.java` 中：

```java
// 原
log.debug("MCP >> {}", reqJson);
log.debug("MCP << {}", respBody);

// 改为
log.debug("MCP >> method={}, params={}", method, LogSanitizer.sanitize(params));
log.debug("MCP << {}", LogSanitizer.sanitize(respBody));
```

或在记录前移除 `authHeaders` 字段。

- [ ] **Step 8: SEC-06 + SEC-10 — LlmHttpClient 安全改进**

SEC-06 (用户消息脱敏)：

```java
// mapException() 中
// 原
throw new LlmApiException("认证失败: " + responseBody);
// 改为
log.error("LLM API 认证失败: {}", responseBody);
throw new LlmApiException("AI服务暂时不可用，请稍后重试");
```

为 `LlmApiException` 增加 `internalMessage` 字段区分内部日志和用户显示。

SEC-10 (OkHttp 日志级别)：

```java
// 原
HttpLoggingInterceptor logging = new HttpLoggingInterceptor(log::debug);
logging.setLevel(HttpLoggingInterceptor.Level.BASIC); // 会记录 Authorization 头

// 改为
HttpLoggingInterceptor logging = new HttpLoggingInterceptor(log::debug);
logging.setLevel(HttpLoggingInterceptor.Level.HEADERS);
// 添加自定义拦截器移除 Authorization 头再记录
```

或直接在 `application.yml` 中将 `com.hify.common.http` 日志级别设为 `INFO`。

- [ ] **Step 9: SEC-16 — KnowledgeServiceImpl 安全注释**

在 `validateUploadFile()` 方法上方添加：

```java
/**
 * 文件上传安全校验（防御完备）：
 * - 路径穿越防护：拒绝 ..\\、../、\
 * - 文件类型白名单：pdf/docx/txt/md/csv/json
 * - 魔术数字验证：校验文件头字节
 * - 大小限制：50MB
 * 新增文件类型时需同步更新白名单和魔术数字表。
 */
```

- [ ] **Step 10: SEC-17 — UrlSecurityValidator RFC 1918 注释**

```java
// 校验 RFC 1918 私有网段：172.16.0.0 - 172.31.255.255（含 Docker 默认 172.17.0.0/16）
```

- [ ] **Step 11: DUP-10 — saveNodes 委托 saveOneNode**

```java
for (WorkflowNodeEntity node : nodes) {
    WorkflowNodeEntity saved = saveOneNode(node, workflowId);
    if (saved != null) {
        savedNodes.add(saved);
    }
}
```

- [ ] **Step 12: DUP-14 — HifyTable 统一 datetime 格式化**

在 `HifyTable.vue` 列渲染添加 `type: 'datetime'` 分支，各列表视图日期列定义添加 `type: 'datetime'`，删除模板中手动 `formatDateTime()`。

- [ ] **Step 13: DUP-17 — WorkflowList 改用 useConfirm**

```typescript
import { useConfirm } from '@/composables/useConfirm'
const { confirmDelete } = useConfirm()
```

将 `ElMessageBox.confirm(...)` 替换为 `confirmDelete(...)`。

- [ ] **Step 14: 编译验证**

```bash
mvn compile -DskipTests
cd hify-web && npm run build
```

- [ ] **Step 15: Commit**

```bash
git add pom.xml hify-app/ hify-common/ hify-knowledge/ hify-mcp/ hify-workflow/ hify-web/
git commit -m "chore: Phase 1 收尾 — SEC-02~07/10/11/15/16/17 + DUP-10/14/17 + MySQL connector 8.4.0"
```

---

## Phase 2：JWT 认证 + 安全基础设施

> 从原 Phase 5 前移。理由：P0 安全项不应最后。认证是其他所有安全措施的前提。

### Task 2.1: SEC-01 — JWT 认证基础设施

**Files:**
- Modify: `pom.xml`（添加 security + jjwt 依赖）
- Modify: `hify-app/src/main/resources/application.yml`（JWT 配置）
- Modify: `hify-app/src/main/resources/db/schema.sql`（users 表）
- Create: `hify-app/src/main/java/com/hify/config/SecurityConfig.java`
- Create: `hify-app/src/main/java/com/hify/security/JwtUtils.java`
- Create: `hify-app/src/main/java/com/hify/security/JwtAuthFilter.java`
- Create: `hify-app/src/main/java/com/hify/controller/AuthController.java`
- Create: `hify-app/src/main/java/com/hify/service/AuthService.java`
- Create: `hify-app/src/main/java/com/hify/service/impl/AuthServiceImpl.java`
- Create: `hify-app/src/main/java/com/hify/dto/LoginReq.java`
- Create: `hify-app/src/main/java/com/hify/dto/LoginResp.java`
- Create: `hify-app/src/main/java/com/hify/dto/RegisterReq.java`
- Modify: `hify-web/src/router/index.ts`（路由守卫）
- Create: `hify-web/src/views/Login.vue`（登录页）

- [ ] **Step 1: 添加依赖**

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

- [ ] **Step 2: application.yml JWT 配置**

```yaml
jwt:
  secret: ${JWT_SECRET:default-jwt-secret-change-in-production-min-32-bytes}
  expiration: 86400000  # 24 hours
```

- [ ] **Step 3: schema.sql users 表**

```sql
-- INFORMATION_SCHEMA 检查后动态执行
ALTER TABLE users ADD COLUMN password_hash VARCHAR(255) NOT NULL DEFAULT '';
```

- [ ] **Step 4: 新建 JwtUtils**

```java
package com.hify.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtils {
    private final SecretKey key;
    private final long expiration;

    public JwtUtils(@Value("${jwt.secret}") String secret,
                    @Value("${jwt.expiration}") long expiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    public String generateToken(Long userId, String username) {
        Date now = new Date();
        return Jwts.builder()
            .subject(String.valueOf(userId))
            .claim("username", username)
            .issuedAt(now)
            .expiration(new Date(now.getTime() + expiration))
            .signWith(key)
            .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser().verifyWith(key).build()
            .parseSignedClaims(token).getPayload();
    }

    public Long getUserId(String token) {
        return Long.valueOf(parseToken(token).getSubject());
    }
}
```

- [ ] **Step 5: 新建 JwtAuthFilter**

```java
package com.hify.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String token = extractToken(request);
        if (token != null) {
            try {
                Long userId = jwtUtils.getUserId(token);
                UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception e) {
                log.debug("JWT 验证失败: {}", e.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
```

- [ ] **Step 6: 新建 SecurityConfig**

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // JWT 无状态认证，天然免疫 CSRF → 显式禁用
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/**").authenticated()
                .anyRequest().permitAll())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

- [ ] **Step 7: 新建 AuthController / AuthService / DTO**

```java
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginResp> login(@RequestBody @Valid LoginReq req) {
        return Result.ok(authService.login(req));
    }

    @PostMapping("/register")
    public Result<LoginResp> register(@RequestBody @Valid RegisterReq req) {
        return Result.ok(authService.register(req));
    }

    @PostMapping("/refresh")
    public Result<LoginResp> refresh(@RequestHeader("Authorization") String token) {
        return Result.ok(authService.refresh(token));
    }
}
```

- [ ] **Step 8: 前端路由守卫 + 登录页**

`router/index.ts` 添加 `beforeEach` 守卫检查 `localStorage` 中 token。`Login.vue` 提供登录表单。

前端 axios 拦截器已处理 token 注入（`request.ts` 从 `localStorage` 取 `hify_token`），需确保与后端 `Authorization: Bearer <token>` 格式一致。SSE fetch 已手动取 token，需统一检查。

- [ ] **Step 9: 编译 + 启动验证**

```bash
mvn compile -DskipTests
# curl -X POST http://localhost:8080/api/v1/agents → 401
# curl -X POST http://localhost:8080/api/v1/auth/login -d '{"username":"admin","password":"xxx"}' → token
# curl -H "Authorization: Bearer <token>" http://localhost:8080/api/v1/agents → 200
```

- [ ] **Step 10: Commit**

```bash
git add pom.xml hify-app/ hify-web/
git commit -m "feat: SEC-01 JWT 认证系统 — SecurityConfig + JwtAuthFilter + AuthController + 登录页"
```

---

### Task 2.2: SEC-09 — 全局限流拦截器 (按 IP)

**Files:**
- Create: `hify-app/src/main/java/com/hify/config/RateLimitInterceptor.java`
- Modify: `hify-app/src/main/java/com/hify/config/WebMvcConfig.java`

**注意：** 原计划使用单例 `RateLimiter` 计数所有请求 → 一个恶意用户拖垮全体。修复为按 IP 分片。

- [ ] **Step 1: 新建 RateLimitInterceptor (按 IP)**

```java
package com.hify.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final ConcurrentHashMap<String, RateLimiter> limiters = new ConcurrentHashMap<>();
    private final RateLimiterConfig config = RateLimiterConfig.custom()
        .limitForPeriod(60)
        .limitRefreshPeriod(Duration.ofMinutes(1))
        .timeoutDuration(Duration.ofMillis(0))
        .build();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String ip = request.getRemoteAddr();
        RateLimiter limiter = limiters.computeIfAbsent(ip,
            k -> RateLimiterRegistry.of(config).rateLimiter(k));
        if (!limiter.acquirePermission()) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁，请稍后重试\"}");
            return false;
        }
        return true;
    }
}
```

- [ ] **Step 2: WebMvcConfig 注册拦截器**

```java
@Override
public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(rateLimitInterceptor)
        .addPathPatterns("/api/v1/**")
        .excludePathPatterns("/api/v1/auth/**");
}
```

- [ ] **Step 3: Commit**

```bash
git add hify-app/
git commit -m "feat: SEC-09 全局限流拦截器（按 IP 分片）"
```

---

## Phase 3：提取共享 + 小方法拆分

### Task 3.1: DUP-06/09 — toJson 统一 + BasePageParams DTO

（内容与原 Task 2.1 相同，编号调整为 Phase 3）

**Files:**
- Create: `hify-common/src/main/java/com/hify/common/dto/BasePageParams.java`
- Modify: `hify-agent/src/main/java/com/hify/agent/dto/AgentListParams.java`
- Modify: `hify-mcp/src/main/java/com/hify/mcp/dto/McpServerListParams.java`
- Modify: `hify-workflow/src/main/java/com/hify/workflow/dto/WorkflowListParams.java`

- [ ] **Step 1: 确认 toJson 调用已统一为 JsonUtils.toJson()**

- [ ] **Step 2: 新建 BasePageParams**

```java
package com.hify.common.dto;

import lombok.Data;

@Data
public class BasePageParams {
    private Integer page = 1;
    private Integer pageSize = 10;
    private String name;
    private String status;
    private String sortField;
    private String sortOrder;
}
```

- [ ] **Step 3: 各模块 ListParams 继承 BasePageParams**

```java
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentListParams extends BasePageParams {
    private Long modelConfigId;
}
```

- [ ] **Step 4: 编译验证 + Commit**

```bash
mvn compile -DskipTests
git add hify-common/ hify-agent/ hify-mcp/ hify-workflow/
git commit -m "refactor: 提取 BasePageParams DTO 统一分页参数"
```

---

### Task 3.2: CPX-16~24 — 小方法拆分（一批处理）

（内容与原 Task 2.2 相同，编号调整为 Phase 3）

**Files:** 同原 Task 2.2

**Steps:** 同原 Task 2.2 (CPX-16 WorkflowServiceImpl.update → CPX-24 ProviderServiceImpl.delete)

- [ ] **编译 + 测试 + Commit**

```bash
mvn compile -DskipTests && mvn test
git add hify-workflow/ hify-common/ hify-provider/ hify-agent/
git commit -m "refactor: CPX-16~24 小方法拆分，降低圈复杂度"
```

---

## Phase 4：大类拆分

（内容与原 Phase 3 相同，编号调整为 Phase 4）

### Task 4.1: CPX-11 — ProviderServiceImpl 拆分
### Task 4.2: CPX-12 — AgentServiceImpl 拆分
### Task 4.3: CPX-13 — KnowledgeServiceImpl 拆分
### Task 4.4: CPX-06 + CPX-01/02 — ChatServiceImpl 上帝类拆分

**注意：** ChatServiceImpl 拆分风险高。分两步执行：
- Step 1-3: 先提取 `SessionManager` + `AgentContextResolver`（纯读操作，低风险）。编译验证。
- Step 4-7: 再提取 `ChatOrchestrator` + `MessageBuilder`（核心对话循环，高风险）。充分测试。

### Task 4.5: CPX-03/04/05 — WorkflowEngine 参数对象 + LlmNodeExecutor 收尾

---

## Phase 5：前端组件拆分 + 安全加固收尾

> 已裁剪：BaseController/BaseServiceImpl（过度抽象）、crudApi 工厂/CrudListPage（ROI 低）。
> 保留：MapStruct 映射器（pom.xml 已有依赖，创建各模块 Converter 接口即可）。

### Task 5.1: CPX-07 — WorkflowEditor.vue 拆分

（内容与原 Task 4.1 相同）

### Task 5.2: CPX-08/26/25 — AgentList + ProviderList + ChatView 拆分

（内容与原 Task 4.2 相同）

### Task 5.3: DUP-15 — MapStruct 替代 BeanUtils.copyProperties

**注意：** pom.xml 已有 MapStruct 1.5.5.Final 依赖，无需添加。仅在各模块创建 Converter 接口。

在各模块新建 Mapper 接口：

```java
// hify-agent/src/main/java/com/hify/agent/converter/AgentConverter.java
package com.hify.agent.converter;

import com.hify.agent.dto.AgentResp;
import com.hify.agent.entity.AgentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import java.util.List;

@Mapper
public interface AgentConverter {
    AgentConverter INSTANCE = Mappers.getMapper(AgentConverter.class);
    AgentResp toResponse(AgentEntity entity);
    List<AgentResp> toResponseList(List<AgentEntity> entities);
}
```

各 ServiceImpl 中用 `AgentConverter.INSTANCE.toResponse(entity)` 替代 `BeanUtils.copyProperties(entity, resp)`。

Provider/MCP/Workflow/Knowledge 模块同理。

- [ ] **编译验证**

```bash
mvn compile -DskipTests
mvn test  # 确保 Entity→Response 映射结果一致
```

- [ ] **Commit**

```bash
git add hify-agent/ hify-provider/ hify-mcp/ hify-workflow/ hify-knowledge/
git commit -m "refactor: DUP-15 MapStruct 替代 BeanUtils.copyProperties"
```

---

### Task 5.4: SEC-12/13/14 — 安全加固收尾

**Files:**
- Modify: `hify-common/.../util/UrlSecurityValidator.java` (SEC-12 DNS 二次校验)
- Modify: `hify-common/.../http/LlmHttpClient.java` (接入 SafeDns)
- Modify: `hify-web/src/views/provider/ProviderList.vue` (SEC-13)
- Modify: `hify-web/src/views/provider/ProviderFormDialog.vue` (SEC-13)

- [ ] **Step 1: SEC-12 — DNS 二次校验 + SafeDns**

在 `UrlSecurityValidator` 中：校验时缓存解析 IP → 返回缓存供 OkHttp 使用。

新建 `SafeDns implements Dns`（OkHttp 接口），在 `lookup()` 时比对当前解析结果与缓存，不匹配则拒绝。

修改 `LlmHttpClient` 构造函数，注入 `SafeDns` 构建 `OkHttpClient`：

```java
public LlmHttpClient(SafeDns safeDns, ...) {
    this.okHttpClient = new OkHttpClient.Builder()
        .dns(safeDns)
        // ... 其他配置
        .build();
}
```

**已知局限：** `EmbeddingService` 自建 HTTP 连接不走 OkHttp，不受 SafeDns 保护。当前 `embeddingUrl` 来自配置文件（可控），风险较低。后续如支持用户自定义 Embedding URL，需统一 HTTP 客户端。

- [ ] **Step 2: SEC-13 — API Key 输入框 password 类型**

`ProviderFormDialog.vue` 中：

```vue
<el-form-item label="API Key">
  <el-input v-model="form.apiKey" type="password" show-password
    placeholder="输入 API Key，留空则不修改" />
</el-form-item>
```

- [ ] **Step 3: 编译验证**

```bash
mvn compile -DskipTests
cd hify-web && npm run build
```

- [ ] **Step 4: Commit**

```bash
git add hify-common/ hify-web/
git commit -m "feat: SEC-12/13 DNS 二次校验 + API Key 掩码"
```

---

## 验证清单

### Phase 1 验证

- [ ] `mvn compile -DskipTests` 无错误
- [ ] hify-provider/hify-chat/hify-workflow 测试通过
- [ ] Provider 健康检查调度正常，并发竞态安全
- [ ] 聊天工具调用功能不变（ToolCallHandler 接入后）
- [ ] 工作流 LLM 节点工具调用不变
- [ ] SSRF URL 校验覆盖 EmbeddingService
- [ ] MCP 日志不泄露 API Key
- [ ] LLM 错误消息不泄露上游响应体
- [ ] HttpClient 日志不记录 Authorization 头（或该包日志级别 INFO）
- [ ] 前端 `npm run build` 无错误
- [ ] XSS 测试：`<script>alert(1)</script>` / `<img src=x onerror=alert(1)>` 不触发

### Phase 2 验证

- [ ] `mvn compile -DskipTests` 无错误
- [ ] 未认证请求 → 401
- [ ] POST /api/v1/auth/login 正确凭据 → token
- [ ] 错误凭据 → 401
- [ ] 有效 token → 所有 API 正常
- [ ] 过期 token → 401
- [ ] SSE 端点认证正常（fetch 手动带 Authorization header）
- [ ] 429 限流生效（单 IP 60次/分钟 POST/PUT/DELETE）
- [ ] 不同 IP 限流互不影响

### Phase 3 验证

- [ ] `mvn compile -DskipTests` 无错误
- [ ] Agent/MCP/Workflow 列表 API 参数解析不变
- [ ] LLM 流式响应正常
- [ ] 熔断重试退避算法行为一致
- [ ] Provider 连接测试正常
- [ ] Provider/Agent 分页列表返回数据一致

### Phase 4 验证

- [ ] 所有模块 `mvn test` 通过
- [ ] Provider 模型同步功能正常
- [ ] Agent 工具/MCP 绑定功能正常
- [ ] 知识库上传/查询功能正常
- [ ] 聊天全部模式（LLM/工具调用/工作流）正常
- [ ] SSE 流式响应正常
- [ ] 工作流 DAG 执行（顺序/条件/错误路由）正常

### Phase 5 验证

- [ ] `cd hify-web && npm run build` 无错误
- [ ] 工作流编辑器：4 种节点配置面板正常、边配置正常
- [ ] Agent 创建/编辑对话框正常
- [ ] Provider 创建/编辑/健康状态显示正常
- [ ] 聊天 SSE 流式功能不变
- [ ] Entity→Response 映射结果一致（MapStruct）
- [ ] DNS 重绑定检测正常
- [ ] API Key 输入框为密码类型

---

## 进度追踪

| 阶段 | 内容 | 目标提交数 | 状态 |
|------|------|-----------|------|
| Phase 1 | 收尾 + S 项批量处理（含 SEC-02~07 补漏） | 6 | 待开始 |
| Phase 2 | JWT 认证 + 限流（P0 安全前置） | 2 | 待开始 |
| Phase 3 | 提取共享 + 小方法拆分 | 2 | 待开始 |
| Phase 4 | 大类拆分 | 5 | 待开始 |
| Phase 5 | 前端组件拆分 + 安全收尾 | 4 | 待开始 |

## 已裁剪项

| 项 | 理由 |
|-----|------|
| DUP-07/08 BaseController/BaseServiceImpl | 各模块 create/update/delete 逻辑差异大，泛型 CRUD 基类过度抽象 |
| DUP-11 crudApi 泛型工厂 | 仅省 3-5 行样板/文件，ROI 低 |
| DUP-13 CrudListPage 泛型组件 | 5 个列表页筛选器/操作差异大，通用封装无实质价值 |
