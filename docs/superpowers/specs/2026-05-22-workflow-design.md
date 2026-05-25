# Hify 工作流引擎设计文档

> 2026-05-22 | 状态：待实现

## 概述

为 Hify 平台添加简版工作流能力，支持表单式配置、DAG 执行引擎、变量传递和异常处理。不做拖拽编排。

### 核心需求

- 节点类型：LLM 调用、条件分支、RAG 检索、HTTP 请求
- 表单式创建/编辑（非拖拽）
- 节点间变量传递（`{{node_id.field}}` 语法）
- 失败重试 + 异常分支（try-catch 模式）
- Agent 可绑定工作流（对话自动触发），也可 API 直接调用

---

## 数据模型

### workflow（工作流定义）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| name | VARCHAR(128) | 名称 |
| description | VARCHAR(512) | 描述 |
| status | TINYINT | 0=禁用 1=启用 |
| created_at | DATETIME(3) | |
| updated_at | DATETIME(3) | |
| deleted | TINYINT(1) | 逻辑删除 |

### workflow_node（节点）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| workflow_id | BIGINT | 所属工作流 |
| name | VARCHAR(64) | 节点名称 |
| type | VARCHAR(16) | llm / condition / rag / http |
| config_json | JSON | 节点配置（各类型不同） |
| position_x | INT | 节点坐标 |
| position_y | INT | 节点坐标 |
| created_at | DATETIME(3) | |
| updated_at | DATETIME(3) | |
| deleted | TINYINT(1) | |

### workflow_edge（连线）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| workflow_id | BIGINT | 所属工作流 |
| source_node_id | BIGINT | 源节点 |
| target_node_id | BIGINT | 目标节点 |
| edge_type | VARCHAR(16) | normal / true / false / error |
| condition_expr | VARCHAR(512) | 条件表达式（true/false 边时用） |
| sort_order | INT | 排序 |
| created_at | DATETIME(3) | |

### workflow_instance（执行实例）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| workflow_id | BIGINT | 工作流 ID |
| session_id | BIGINT | 对话会话 ID（Agent 触发时有值） |
| trigger_type | VARCHAR(16) | agent / api |
| status | VARCHAR(16) | running / success / failed |
| input_json | JSON | 输入参数 |
| output_json | JSON | 最终输出 |
| error_msg | VARCHAR(500) | 失败原因 |
| started_at | DATETIME(3) | |
| finished_at | DATETIME(3) | |
| created_at | DATETIME(3) | |

### node_execution（节点执行记录）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| instance_id | BIGINT | 执行实例 ID |
| node_id | BIGINT | 节点 ID |
| status | VARCHAR(16) | running / success / failed / skipped |
| input_json | JSON | 节点输入 |
| output_json | JSON | 节点输出 |
| error_msg | VARCHAR(500) | 错误信息 |
| retry_count | INT | 已重试次数 |
| started_at | DATETIME(3) | |
| finished_at | DATETIME(3) | |
| created_at | DATETIME(3) | |

### Agent 表新增字段

```sql
ALTER TABLE agent ADD COLUMN workflow_id BIGINT DEFAULT NULL COMMENT '绑定工作流 ID';
```

---

## API 设计

### Workflow CRUD

```
GET    /api/v1/workflows              # 分页列表
POST   /api/v1/workflows              # 创建（含节点+连线）
GET    /api/v1/workflows/{id}         # 详情（含节点+连线）
PUT    /api/v1/workflows/{id}         # 更新（全量替换节点+连线）
DELETE /api/v1/workflows/{id}         # 逻辑删除
```

节点和连线随工作流一起提交，不单独提供 CRUD 接口。

### Workflow 执行

```
POST   /api/v1/workflows/{id}/run     # 触发执行，返回 instance_id
GET    /api/v1/workflows/runs         # 执行历史列表（分页）
GET    /api/v1/workflows/runs/{id}    # 执行详情（含各节点执行记录）
```

执行是同步的：请求等待工作流全部完成后返回结果。

---

## 执行引擎

### DAG 调度

1. 加载所有节点和连线，构建邻接表
2. 拓扑排序，验证无环
3. 找到入度为 0 的起点
4. 按拓扑序执行，根据出边类型决定跳转：
   - `normal`：继续下一节点
   - `true/false`：条件节点评估后选择
   - `error`：当前节点失败时跳转

### 节点执行器（策略模式）

定义 `NodeExecutor` 接口，四种实现：

| 类型 | 职责 | 依赖 |
|------|------|------|
| llm | 调用 ProviderAdapterFactory 调 LLM | hify-provider |
| rag | 调用 KnowledgeService.query() | hify-knowledge |
| http | OkHttp 发送 HTTP 请求 | OkHttp |
| condition | 评估表达式，返回 true/false | - |

### 变量传递

执行时维护 `Map<String, Object> variables`（key = node_id）。配置中的 `{{node_id.field}}` 在执行前替换为实际值。

### 重试与异常分支

- 每个节点可配置 `max_retries`（默认 0）
- 所有重试失败后检查 `error` 出边：有则跳转异常处理节点，无则工作流标记失败

### Chat 模块集成

```
用户发消息 → 保存用户消息 → 检查 Agent.workflow_id
  ├─ 有值 → 执行工作流（input={user_message, session_id}）
  │         工作流输出作为 assistant 消息保存
  └─ 无值 → 走现有直接 LLM 调用
```

---

## 前端

### 路由

- `/workflows` — 工作流列表
- `/workflows/create` — 新建工作流
- `/workflows/:id/edit` — 编辑工作流
- 侧边菜单新增"工作流"项

### 编辑器布局（表单式，非拖拽）

- **顶部**：名称、描述输入框
- **左侧**：已添加节点列表（卡片形式）
- **右侧**：选中节点的配置表单
  - LLM 节点：选模型 + prompt 模板
  - RAG 节点：选知识库 + query
  - HTTP 节点：URL、方法、Headers、Body
  - 条件节点：表达式输入
- **连线配置**（节点配置区下方）：下一节点、条件分支、异常处理跳转

全部使用 Element Plus 组件，无额外依赖。

---

## 实施范围

### 后端
- hify-workflow 模块：Entity × 5、Mapper × 5、DTO、Controller、Service、执行引擎
- hify-chat 模块：ChatServiceImpl 增加工作流触发分支
- hify-agent 模块：AgentEntity 新增 workflow_id 字段
- hify-app：schema.sql 新增 5 张表

### 前端
- 新增 `views/workflow/` 目录（列表页 + 编辑器）
- 新增 `api/workflow.ts`
- 路由和菜单更新
- 类型定义补充

---

## 验证方式

1. 启动后端，确认 5 张新表自动创建
2. 通过 API 创建包含 4 种节点的工作流，验证 CRUD
3. 通过 `POST /run` 执行工作流，验证 LLM/RAG/HTTP/条件节点正常运行
4. 模拟节点失败，验证重试和异常分支跳转
5. Agent 绑定工作流后，通过对话触发，验证端到端流程
6. 前端表单创建和编辑工作流，验证交互流程
