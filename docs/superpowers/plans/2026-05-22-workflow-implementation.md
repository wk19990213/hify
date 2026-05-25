# Hify 工作流引擎实现计划

> **For agentic workers:** 按任务编号顺序执行，每个任务独立提交。步骤使用 `- [ ]` checkbox 跟踪。

**目标:** 为 Hify 平台实现基于 DAG 的简版工作流引擎，支持 LLM/RAG/HTTP/条件四种节点，表单式编辑，变量传递和异常分支。

**架构:** 关系型表存储工作流定义，DAG 调度引擎按拓扑序执行节点，策略模式实现四种节点执行器，Chat 模块检测 Agent 绑定的工作流后自动触发。

**技术栈:** Spring Boot 3.x + MyBatis-Plus + MySQL 8.x + Vue 3 + TypeScript + Element Plus

---

### Task 1: 数据库 DDL

**Files:**
- Modify: `hify-app/src/main/resources/db/schema.sql`

- [ ] **Step 1: 在 schema.sql 末尾添加 5 张工作流表的 DDL**

```sql
-- ----------------------------
-- 9. 工作流定义
-- ----------------------------
CREATE TABLE IF NOT EXISTS workflow (
    id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    name           VARCHAR(128)  NOT NULL COMMENT '工作流名称',
    description    VARCHAR(512)  DEFAULT '' COMMENT '描述',
    status         TINYINT(1)    NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    created_at     DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at     DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted        TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    INDEX idx_status (status),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作流定义';

-- ----------------------------
-- 10. 工作流节点
-- ----------------------------
CREATE TABLE IF NOT EXISTS workflow_node (
    id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    workflow_id    BIGINT        NOT NULL COMMENT '所属工作流 ID',
    name           VARCHAR(64)   NOT NULL COMMENT '节点名称',
    type           VARCHAR(16)   NOT NULL COMMENT '节点类型: llm/condition/rag/http',
    config_json    JSON          DEFAULT NULL COMMENT '节点配置 JSON',
    position_x     INT           DEFAULT 0 COMMENT 'X 坐标',
    position_y     INT           DEFAULT 0 COMMENT 'Y 坐标',
    created_at     DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at     DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted        TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    INDEX idx_workflow_id (workflow_id),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作流节点';

-- ----------------------------
-- 11. 工作流连线
-- ----------------------------
CREATE TABLE IF NOT EXISTS workflow_edge (
    id              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    workflow_id     BIGINT        NOT NULL COMMENT '所属工作流 ID',
    source_node_id  BIGINT        NOT NULL COMMENT '源节点 ID',
    target_node_id  BIGINT        NOT NULL COMMENT '目标节点 ID',
    edge_type       VARCHAR(16)   NOT NULL DEFAULT 'normal' COMMENT 'normal/true/false/error',
    condition_expr  VARCHAR(512)  DEFAULT NULL COMMENT '条件表达式',
    sort_order      INT           DEFAULT 0 COMMENT '排序',
    created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_workflow_id (workflow_id),
    INDEX idx_source_node (source_node_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作流连线';

-- ----------------------------
-- 12. 工作流执行实例
-- ----------------------------
CREATE TABLE IF NOT EXISTS workflow_instance (
    id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    workflow_id    BIGINT        NOT NULL COMMENT '工作流 ID',
    session_id     BIGINT        DEFAULT NULL COMMENT '对话会话 ID',
    trigger_type   VARCHAR(16)   NOT NULL DEFAULT 'api' COMMENT '触发类型: agent/api',
    status         VARCHAR(16)   NOT NULL DEFAULT 'running' COMMENT 'running/success/failed',
    input_json     JSON          DEFAULT NULL COMMENT '输入参数 JSON',
    output_json    JSON          DEFAULT NULL COMMENT '最终输出 JSON',
    error_msg      VARCHAR(500)  DEFAULT NULL COMMENT '失败原因',
    started_at     DATETIME(3)   DEFAULT NULL COMMENT '开始时间',
    finished_at    DATETIME(3)   DEFAULT NULL COMMENT '完成时间',
    created_at     DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_workflow_id (workflow_id),
    INDEX idx_status (status),
    INDEX idx_session_id (session_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作流执行实例';

-- ----------------------------
-- 13. 节点执行记录
-- ----------------------------
CREATE TABLE IF NOT EXISTS node_execution (
    id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    instance_id    BIGINT        NOT NULL COMMENT '执行实例 ID',
    node_id        BIGINT        NOT NULL COMMENT '节点 ID',
    status         VARCHAR(16)   NOT NULL DEFAULT 'running' COMMENT 'running/success/failed/skipped',
    input_json     JSON          DEFAULT NULL COMMENT '节点输入 JSON',
    output_json    JSON          DEFAULT NULL COMMENT '节点输出 JSON',
    error_msg      VARCHAR(500)  DEFAULT NULL COMMENT '错误信息',
    retry_count    INT           DEFAULT 0 COMMENT '已重试次数',
    started_at     DATETIME(3)   DEFAULT NULL COMMENT '开始时间',
    finished_at    DATETIME(3)   DEFAULT NULL COMMENT '完成时间',
    created_at     DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_instance_id (instance_id),
    INDEX idx_node_id (node_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='节点执行记录';

-- ----------------------------
-- 14. Agent 新增 workflow_id 字段
-- ----------------------------
ALTER TABLE agent ADD COLUMN IF NOT EXISTS workflow_id BIGINT DEFAULT NULL COMMENT '绑定工作流 ID';
```

- [ ] **Step 2: 提交**

```bash
git add hify-app/src/main/resources/db/schema.sql
git commit -m "feat: 添加工作流相关数据库表 DDL"
```

---

### Task 2: Workflow 实体类

**Files:**
- Modify: `hify-workflow/src/main/java/com/hify/workflow/entity/WorkflowEntity.java`
- Create: `hify-workflow/src/main/java/com/hify/workflow/entity/WorkflowNodeEntity.java`
- Create: `hify-workflow/src/main/java/com/hify/workflow/entity/WorkflowEdgeEntity.java`
- Create: `hify-workflow/src/main/java/com/hify/workflow/entity/WorkflowInstanceEntity.java`
- Create: `hify-workflow/src/main/java/com/hify/workflow/entity/NodeExecutionEntity.java`

- [ ] **Step 1: 重写 WorkflowEntity.java**

```java
package com.hify.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hify.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("workflow")
public class WorkflowEntity extends BaseEntity {
    private String name;
    private String description;
    private Integer status;
}
```

- [ ] **Step 2: 创建 WorkflowNodeEntity.java**

```java
package com.hify.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hify.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("workflow_node")
public class WorkflowNodeEntity extends BaseEntity {
    private Long workflowId;
    private String name;
    private String type;
    private String configJson;
    private Integer positionX;
    private Integer positionY;
}
```

- [ ] **Step 3: 创建 WorkflowEdgeEntity.java**

```java
package com.hify.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("workflow_edge")
public class WorkflowEdgeEntity {
    private Long id;
    private Long workflowId;
    private Long sourceNodeId;
    private Long targetNodeId;
    private String edgeType;
    private String conditionExpr;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 4: 创建 WorkflowInstanceEntity.java**

```java
package com.hify.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("workflow_instance")
public class WorkflowInstanceEntity {
    private Long id;
    private Long workflowId;
    private Long sessionId;
    private String triggerType;
    private String status;
    private String inputJson;
    private String outputJson;
    private String errorMsg;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 5: 创建 NodeExecutionEntity.java**

```java
package com.hify.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("node_execution")
public class NodeExecutionEntity {
    private Long id;
    private Long instanceId;
    private Long nodeId;
    private String status;
    private String inputJson;
    private String outputJson;
    private String errorMsg;
    private Integer retryCount;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 6: 提交**

```bash
git add hify-workflow/src/main/java/com/hify/workflow/entity/
git commit -m "feat: 添加工作流 5 个实体类"
```

---

### Task 3: Workflow Mapper

**Files:**
- Modify: `hify-workflow/src/main/java/com/hify/workflow/mapper/WorkflowMapper.java`
- Create: `hify-workflow/src/main/java/com/hify/workflow/mapper/WorkflowNodeMapper.java`
- Create: `hify-workflow/src/main/java/com/hify/workflow/mapper/WorkflowEdgeMapper.java`
- Create: `hify-workflow/src/main/java/com/hify/workflow/mapper/WorkflowInstanceMapper.java`
- Create: `hify-workflow/src/main/java/com/hify/workflow/mapper/NodeExecutionMapper.java`

- [ ] **Step 1: 重写 WorkflowMapper.java**

```java
package com.hify.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hify.workflow.entity.WorkflowEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkflowMapper extends BaseMapper<WorkflowEntity> {
}
```

- [ ] **Step 2-5: 创建其余 4 个 Mapper（同理）**

WorkflowNodeMapper.java:
```java
package com.hify.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hify.workflow.entity.WorkflowNodeEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkflowNodeMapper extends BaseMapper<WorkflowNodeEntity> {
}
```

WorkflowEdgeMapper.java:
```java
package com.hify.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hify.workflow.entity.WorkflowEdgeEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkflowEdgeMapper extends BaseMapper<WorkflowEdgeEntity> {
}
```

WorkflowInstanceMapper.java:
```java
package com.hify.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hify.workflow.entity.WorkflowInstanceEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkflowInstanceMapper extends BaseMapper<WorkflowInstanceEntity> {
}
```

NodeExecutionMapper.java:
```java
package com.hify.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hify.workflow.entity.NodeExecutionEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NodeExecutionMapper extends BaseMapper<NodeExecutionEntity> {
}
```

- [ ] **Step 6: 提交**

```bash
git add hify-workflow/src/main/java/com/hify/workflow/mapper/
git commit -m "feat: 添加工作流 5 个 Mapper 接口"
```

---

### Task 4: Workflow DTO

**Files:**
- Modify: `hify-workflow/src/main/java/com/hify/workflow/dto/WorkflowDto.java`
- Create: `hify-workflow/src/main/java/com/hify/workflow/dto/WorkflowCreateReq.java`
- Create: `hify-workflow/src/main/java/com/hify/workflow/dto/WorkflowUpdateReq.java`
- Create: `hify-workflow/src/main/java/com/hify/workflow/dto/WorkflowListParams.java`
- Create: `hify-workflow/src/main/java/com/hify/workflow/dto/WorkflowResp.java`
- Create: `hify-workflow/src/main/java/com/hify/workflow/dto/WorkflowRunReq.java`
- Create: `hify-workflow/src/main/java/com/hify/workflow/dto/WorkflowInstanceResp.java`
- Create: `hify-workflow/src/main/java/com/hify/workflow/dto/NodeExecutionResp.java`

- [ ] **Step 1: 创建 WorkflowDto.java（节点+连线子对象）**

```java
package com.hify.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class WorkflowDto {

    @NotBlank(message = "名称不能为空")
    private String name;

    private String description;

    private Integer status;

    @NotNull(message = "节点列表不能为空")
    private List<NodeItem> nodes;

    @NotNull(message = "连线列表不能为空")
    private List<EdgeItem> edges;

    @Data
    public static class NodeItem {
        private String name;
        @NotBlank(message = "节点类型不能为空")
        private String type;        // llm / condition / rag / http
        private String configJson;  // JSON string
        private Integer positionX;
        private Integer positionY;
    }

    @Data
    public static class EdgeItem {
        @NotNull(message = "源节点索引不能为空")
        private Integer sourceNodeIndex;
        @NotNull(message = "目标节点索引不能为空")
        private Integer targetNodeIndex;
        private String edgeType;        // normal / true / false / error
        private String conditionExpr;
        private Integer sortOrder;
    }
}
```

- [ ] **Step 2: 创建 WorkflowResp.java**

```java
package com.hify.workflow.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class WorkflowResp {
    private Long id;
    private String name;
    private String description;
    private Integer status;
    private List<WorkflowDto.NodeItem> nodes;
    private List<WorkflowDto.EdgeItem> edges;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 3: 创建 WorkflowListParams.java**

```java
package com.hify.workflow.dto;

import lombok.Data;

@Data
public class WorkflowListParams {
    private Integer page = 1;
    private Integer pageSize = 20;
    private String name;
    private Integer status;
}
```

- [ ] **Step 4: 创建 WorkflowCreateReq.java**

```java
package com.hify.workflow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class WorkflowCreateReq {
    @NotBlank(message = "名称不能为空")
    private String name;
    private String description;
    private Integer status;

    @NotNull(message = "节点列表不能为空")
    @Valid
    private List<WorkflowDto.NodeItem> nodes;

    @NotNull(message = "连线列表不能为空")
    @Valid
    private List<WorkflowDto.EdgeItem> edges;
}
```

- [ ] **Step 5: 创建 WorkflowUpdateReq.java**

```java
package com.hify.workflow.dto;

import jakarta.validation.Valid;
import lombok.Data;
import java.util.List;

@Data
public class WorkflowUpdateReq {
    private String name;
    private String description;
    private Integer status;
    private List<WorkflowDto.NodeItem> nodes;
    private List<WorkflowDto.EdgeItem> edges;
}
```

- [ ] **Step 6: 创建 WorkflowRunReq.java**

```java
package com.hify.workflow.dto;

import lombok.Data;
import java.util.Map;

@Data
public class WorkflowRunReq {
    private Map<String, Object> input;
    private Long sessionId;
}
```

- [ ] **Step 7: 创建 WorkflowInstanceResp.java**

```java
package com.hify.workflow.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class WorkflowInstanceResp {
    private Long id;
    private Long workflowId;
    private String workflowName;
    private Long sessionId;
    private String triggerType;
    private String status;
    private String inputJson;
    private String outputJson;
    private String errorMsg;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
    private List<NodeExecutionResp> nodeExecutions;
}
```

- [ ] **Step 8: 创建 NodeExecutionResp.java**

```java
package com.hify.workflow.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NodeExecutionResp {
    private Long id;
    private Long instanceId;
    private Long nodeId;
    private String nodeName;
    private String nodeType;
    private String status;
    private String inputJson;
    private String outputJson;
    private String errorMsg;
    private Integer retryCount;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
```

- [ ] **Step 9: 提交**

```bash
git add hify-workflow/src/main/java/com/hify/workflow/dto/
git commit -m "feat: 添加工作流 DTO 类"
```

---

### Task 5: NodeExecutor 接口及四种实现

**Files:**
- Create: `hify-workflow/src/main/java/com/hify/workflow/engine/NodeExecutor.java`
- Create: `hify-workflow/src/main/java/com/hify/workflow/engine/NodeExecContext.java`
- Create: `hify-workflow/src/main/java/com/hify/workflow/engine/NodeExecResult.java`
- Create: `hify-workflow/src/main/java/com/hify/workflow/engine/LlmNodeExecutor.java`
- Create: `hify-workflow/src/main/java/com/hify/workflow/engine/ConditionNodeExecutor.java`
- Create: `hify-workflow/src/main/java/com/hify/workflow/engine/RagNodeExecutor.java`
- Create: `hify-workflow/src/main/java/com/hify/workflow/engine/HttpNodeExecutor.java`
- Modify: `hify-workflow/pom.xml` (add hify-knowledge dependency)

- [ ] **Step 1: pom.xml 新增 hify-knowledge 依赖**

```xml
<!-- 依赖 knowledge 模块 -->
<dependency>
    <groupId>com.hify</groupId>
    <artifactId>hify-knowledge</artifactId>
</dependency>
```

- [ ] **Step 2: 创建 NodeExecContext.java（执行上下文）**

```java
package com.hify.workflow.engine;

import com.hify.workflow.entity.WorkflowNodeEntity;
import lombok.Data;

import java.util.Map;

@Data
public class NodeExecContext {
    private WorkflowNodeEntity node;
    private Map<String, Object> variables;  // 所有已执行节点的输出，key = nodeId
}
```

- [ ] **Step 3: 创建 NodeExecResult.java**

```java
package com.hify.workflow.engine;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NodeExecResult {
    private boolean success;
    private Object output;
    private String errorMsg;
}
```

- [ ] **Step 4: 创建 NodeExecutor 接口**

```java
package com.hify.workflow.engine;

public interface NodeExecutor {
    String getType();
    NodeExecResult execute(NodeExecContext context);
}
```

- [ ] **Step 5: 创建 LlmNodeExecutor.java**

```java
package com.hify.workflow.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hify.common.crypto.AesEncryptor;
import com.hify.provider.adapter.ChatRequest;
import com.hify.provider.adapter.ProviderAdapter;
import com.hify.provider.adapter.ProviderAdapterFactory;
import com.hify.provider.entity.ModelConfigEntity;
import com.hify.provider.entity.ProviderEntity;
import com.hify.provider.entity.ProviderModelEntity;
import com.hify.provider.mapper.ModelConfigMapper;
import com.hify.provider.mapper.ProviderMapper;
import com.hify.provider.mapper.ProviderModelMapper;
import com.hify.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class LlmNodeExecutor implements NodeExecutor {

    private final ModelConfigMapper modelConfigMapper;
    private final ProviderMapper providerMapper;
    private final ProviderModelMapper providerModelMapper;
    private final ProviderAdapterFactory adapterFactory;
    private final ObjectMapper objectMapper;

    @Override
    public String getType() {
        return "llm";
    }

    @Override
    public NodeExecResult execute(NodeExecContext ctx) {
        String configJson = ctx.getNode().getConfigJson();
        Map<String, Object> config;
        try {
            config = objectMapper.readValue(configJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return NodeExecResult.builder().success(false).errorMsg("LLM 节点配置解析失败: " + e.getMessage()).build();
        }

        Long modelConfigId = config.get("modelConfigId") != null
                ? ((Number) config.get("modelConfigId")).longValue() : null;
        String prompt = (String) config.get("prompt");

        if (modelConfigId == null || prompt == null) {
            return NodeExecResult.builder().success(false).errorMsg("LLM 节点缺少模型配置或 Prompt").build();
        }

        // 变量替换
        prompt = resolveVariables(prompt, ctx.getVariables());

        try {
            // 解析模型 → 提供商链（复用 ChatServiceImpl 的逻辑）
            ModelConfigEntity modelConfig = modelConfigMapper.selectById(modelConfigId);
            if (modelConfig == null || modelConfig.getDeleted() == 1) {
                return NodeExecResult.builder().success(false).errorMsg("模型配置不存在").build();
            }

            List<ProviderModelEntity> pmList = providerModelMapper.selectList(
                    new LambdaQueryWrapper<ProviderModelEntity>()
                            .eq(ProviderModelEntity::getModelId, modelConfig.getModelId()));

            ProviderEntity provider = null;
            for (ProviderModelEntity pm : pmList) {
                ProviderEntity p = providerMapper.selectById(pm.getProviderId());
                if (p != null && p.getDeleted() == 0 && p.getStatus() == 1) {
                    provider = p;
                    break;
                }
            }
            if (provider == null) {
                return NodeExecResult.builder().success(false).errorMsg("没有可用的模型提供商").build();
            }

            ProviderAdapter adapter = adapterFactory.getAdapter(provider.getType());

            String authJson = null;
            String encrypted = provider.getAuthConfig();
            if (encrypted != null && !encrypted.isEmpty()) {
                authJson = AesEncryptor.decrypt(encrypted);
            }
            Map<String, Object> authConfig = objectMapper.readValue(authJson, new TypeReference<Map<String, Object>>() {});

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "user", "content", prompt));
            ChatRequest chatReq = new ChatRequest(modelConfig.getModelId(), messages, 0.7, false);
            String response = adapter.chat(provider.getBaseUrl(), authConfig, chatReq);
            String content = adapter.extractContent(response);

            return NodeExecResult.builder().success(true).output(Map.of("content", content)).build();
        } catch (Exception e) {
            log.error("LLM node execution failed: nodeId={}", ctx.getNode().getId(), e);
            return NodeExecResult.builder().success(false).errorMsg("LLM 调用失败: " + e.getMessage()).build();
        }
    }

    private String resolveVariables(String template, Map<String, Object> variables) {
        if (variables == null || variables.isEmpty()) return template;
        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            if (entry.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nested = (Map<String, Object>) entry.getValue();
                for (Map.Entry<String, Object> ne : nested.entrySet()) {
                    result = result.replace("{{" + entry.getKey() + "." + ne.getKey() + "}}",
                            ne.getValue() != null ? ne.getValue().toString() : "");
                }
            }
            result = result.replace("{{" + entry.getKey() + "}}",
                    entry.getValue() != null ? entry.getValue().toString() : "");
        }
        return result;
    }
}
```

- [ ] **Step 6: 创建 ConditionNodeExecutor.java**

```java
package com.hify.workflow.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConditionNodeExecutor implements NodeExecutor {

    private final ObjectMapper objectMapper;

    @Override
    public String getType() {
        return "condition";
    }

    @Override
    public NodeExecResult execute(NodeExecContext ctx) {
        String configJson = ctx.getNode().getConfigJson();
        Map<String, Object> config;
        try {
            config = objectMapper.readValue(configJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return NodeExecResult.builder().success(false).errorMsg("条件节点配置解析失败: " + e.getMessage()).build();
        }

        String expression = (String) config.get("expression");
        if (expression == null || expression.isBlank()) {
            return NodeExecResult.builder().success(false).errorMsg("条件表达式为空").build();
        }

        // 变量替换
        expression = resolveVariables(expression, ctx.getVariables());

        // 简单表达式评估：支持 == 和 !=
        boolean result = evaluateExpression(expression);
        return NodeExecResult.builder().success(true).output(Map.of("result", result)).build();
    }

    private boolean evaluateExpression(String expr) {
        // 支持 == 比较
        if (expr.contains("==")) {
            String[] parts = expr.split("==", 2);
            return parts[0].trim().equals(parts[1].trim());
        }
        // 支持 != 比较
        if (expr.contains("!=")) {
            String[] parts = expr.split("!=", 2);
            return !parts[0].trim().equals(parts[1].trim());
        }
        // 支持布尔值
        return "true".equalsIgnoreCase(expr.trim());
    }

    private String resolveVariables(String template, Map<String, Object> variables) {
        if (variables == null || variables.isEmpty()) return template;
        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            if (entry.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nested = (Map<String, Object>) entry.getValue();
                for (Map.Entry<String, Object> ne : nested.entrySet()) {
                    result = result.replace("{{" + entry.getKey() + "." + ne.getKey() + "}}",
                            ne.getValue() != null ? ne.getValue().toString() : "");
                }
            }
            result = result.replace("{{" + entry.getKey() + "}}",
                    entry.getValue() != null ? entry.getValue().toString() : "");
        }
        return result;
    }
}
```

- [ ] **Step 7: 创建 RagNodeExecutor.java**

```java
package com.hify.workflow.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hify.knowledge.dto.RagResp;
import com.hify.knowledge.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RagNodeExecutor implements NodeExecutor {

    private final KnowledgeService knowledgeService;
    private final ObjectMapper objectMapper;

    @Override
    public String getType() {
        return "rag";
    }

    @Override
    public NodeExecResult execute(NodeExecContext ctx) {
        String configJson = ctx.getNode().getConfigJson();
        Map<String, Object> config;
        try {
            config = objectMapper.readValue(configJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return NodeExecResult.builder().success(false).errorMsg("RAG 节点配置解析失败: " + e.getMessage()).build();
        }

        Long kbId = config.get("kbId") != null ? ((Number) config.get("kbId")).longValue() : null;
        String query = (String) config.get("query");

        if (kbId == null || query == null) {
            return NodeExecResult.builder().success(false).errorMsg("RAG 节点缺少知识库配置或查询语句").build();
        }

        query = resolveVariables(query, ctx.getVariables());

        try {
            RagResp ragResp = knowledgeService.query(kbId, query);
            return NodeExecResult.builder().success(true)
                    .output(Map.of("sources", ragResp.getSources() != null ? ragResp.getSources() : ""))
                    .build();
        } catch (Exception e) {
            log.error("RAG node execution failed: nodeId={}", ctx.getNode().getId(), e);
            return NodeExecResult.builder().success(false).errorMsg("RAG 检索失败: " + e.getMessage()).build();
        }
    }

    private String resolveVariables(String template, Map<String, Object> variables) {
        if (variables == null || variables.isEmpty()) return template;
        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            if (entry.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nested = (Map<String, Object>) entry.getValue();
                for (Map.Entry<String, Object> ne : nested.entrySet()) {
                    result = result.replace("{{" + entry.getKey() + "." + ne.getKey() + "}}",
                            ne.getValue() != null ? ne.getValue().toString() : "");
                }
            }
            result = result.replace("{{" + entry.getKey() + "}}",
                    entry.getValue() != null ? entry.getValue().toString() : "");
        }
        return result;
    }
}
```

- [ ] **Step 8: 创建 HttpNodeExecutor.java**

```java
package com.hify.workflow.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class HttpNodeExecutor implements NodeExecutor {

    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    @Override
    public String getType() {
        return "http";
    }

    @Override
    public NodeExecResult execute(NodeExecContext ctx) {
        String configJson = ctx.getNode().getConfigJson();
        Map<String, Object> config;
        try {
            config = objectMapper.readValue(configJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return NodeExecResult.builder().success(false).errorMsg("HTTP 节点配置解析失败: " + e.getMessage()).build();
        }

        String url = (String) config.get("url");
        String method = config.get("method") != null ? (String) config.get("method") : "GET";
        String body = (String) config.get("body");
        @SuppressWarnings("unchecked")
        Map<String, String> headers = (Map<String, String>) config.get("headers");

        if (url == null) {
            return NodeExecResult.builder().success(false).errorMsg("HTTP 节点缺少 URL").build();
        }

        url = resolveVariables(url, ctx.getVariables());
        if (body != null) {
            body = resolveVariables(body, ctx.getVariables());
        }

        try {
            Request.Builder builder = new Request.Builder().url(url);
            if (headers != null) {
                headers.forEach(builder::addHeader);
            }

            RequestBody requestBody = null;
            if (body != null && !body.isEmpty() && ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method))) {
                requestBody = RequestBody.create(body, MediaType.parse("application/json"));
            }

            if ("GET".equalsIgnoreCase(method)) {
                builder.get();
            } else if ("POST".equalsIgnoreCase(method)) {
                builder.post(requestBody != null ? requestBody : RequestBody.create("", MediaType.parse("application/json")));
            } else if ("PUT".equalsIgnoreCase(method)) {
                builder.put(requestBody != null ? requestBody : RequestBody.create("", MediaType.parse("application/json")));
            } else if ("DELETE".equalsIgnoreCase(method)) {
                builder.delete();
            } else {
                builder.method(method, requestBody);
            }

            try (Response response = httpClient.newCall(builder.build()).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                return NodeExecResult.builder().success(response.isSuccessful())
                        .output(Map.of("status", response.code(), "body", responseBody))
                        .errorMsg(response.isSuccessful() ? null : "HTTP " + response.code())
                        .build();
            }
        } catch (IOException e) {
            log.error("HTTP node execution failed: nodeId={}", ctx.getNode().getId(), e);
            return NodeExecResult.builder().success(false).errorMsg("HTTP 请求失败: " + e.getMessage()).build();
        }
    }

    private String resolveVariables(String template, Map<String, Object> variables) {
        if (variables == null || variables.isEmpty()) return template;
        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            if (entry.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nested = (Map<String, Object>) entry.getValue();
                for (Map.Entry<String, Object> ne : nested.entrySet()) {
                    result = result.replace("{{" + entry.getKey() + "." + ne.getKey() + "}}",
                            ne.getValue() != null ? ne.getValue().toString() : "");
                }
            }
            result = result.replace("{{" + entry.getKey() + "}}",
                    entry.getValue() != null ? entry.getValue().toString() : "");
        }
        return result;
    }
}
```

- [ ] **Step 9: 提交**

```bash
git add hify-workflow/src/main/java/com/hify/workflow/engine/ hify-workflow/pom.xml
git commit -m "feat: 添加四种节点执行器（LLM/RAG/HTTP/Condition）"
```

---

### Task 6: Workflow 引擎（DAG 构建 + 执行调度）

**Files:**
- Create: `hify-workflow/src/main/java/com/hify/workflow/engine/WorkflowEngine.java`

- [ ] **Step 1: 创建 WorkflowEngine.java**

```java
package com.hify.workflow.engine;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hify.common.exception.BizException;
import com.hify.workflow.entity.*;
import com.hify.workflow.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowEngine {

    private final WorkflowMapper workflowMapper;
    private final WorkflowNodeMapper nodeMapper;
    private final WorkflowEdgeMapper edgeMapper;
    private final WorkflowInstanceMapper instanceMapper;
    private final NodeExecutionMapper nodeExecutionMapper;
    private final ObjectMapper objectMapper;
    private final List<NodeExecutor> executors;

    public WorkflowInstanceEntity execute(Long workflowId, Map<String, Object> input, Long sessionId, String triggerType) {
        // 加载工作流定义
        WorkflowEntity workflow = workflowMapper.selectById(workflowId);
        if (workflow == null || workflow.getDeleted() == 1) {
            throw BizException.notFound("工作流不存在");
        }
        if (workflow.getStatus() == 0) {
            throw BizException.paramError("工作流已禁用");
        }

        List<WorkflowNodeEntity> nodes = nodeMapper.selectList(
                new LambdaQueryWrapper<WorkflowNodeEntity>()
                        .eq(WorkflowNodeEntity::getWorkflowId, workflowId)
                        .eq(WorkflowNodeEntity::getDeleted, 0));
        List<WorkflowEdgeEntity> edges = edgeMapper.selectList(
                new LambdaQueryWrapper<WorkflowEdgeEntity>()
                        .eq(WorkflowEdgeEntity::getWorkflowId, workflowId));

        if (nodes.isEmpty()) {
            throw BizException.paramError("工作流没有节点");
        }

        // 构建 DAG 邻接表
        Map<Long, WorkflowNodeEntity> nodeMap = new HashMap<>();
        for (WorkflowNodeEntity node : nodes) {
            nodeMap.put(node.getId(), node);
        }

        // 入度计算
        Map<Long, Integer> inDegree = new HashMap<>();
        for (WorkflowNodeEntity node : nodes) {
            inDegree.put(node.getId(), 0);
        }
        // 出边表: sourceNodeId → List<Edge>
        Map<Long, List<WorkflowEdgeEntity>> outEdges = new HashMap<>();
        for (WorkflowNodeEntity node : nodes) {
            outEdges.put(node.getId(), new ArrayList<>());
        }
        for (WorkflowEdgeEntity edge : edges) {
            outEdges.get(edge.getSourceNodeId()).add(edge);
            inDegree.merge(edge.getTargetNodeId(), 1, Integer::sum);
        }

        // 拓扑排序找起点
        Long startNodeId = null;
        for (Map.Entry<Long, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                startNodeId = entry.getKey();
                break;
            }
        }
        if (startNodeId == null) {
            throw BizException.paramError("工作流存在环路，无法找到起始节点");
        }

        // 创建执行实例
        WorkflowInstanceEntity instance = new WorkflowInstanceEntity();
        instance.setWorkflowId(workflowId);
        instance.setSessionId(sessionId);
        instance.setTriggerType(triggerType);
        instance.setStatus("running");
        try {
            instance.setInputJson(objectMapper.writeValueAsString(input));
        } catch (Exception ignored) {}
        instance.setStartedAt(LocalDateTime.now());
        instanceMapper.insert(instance);

        // 执行 DAG
        Map<String, Object> variables = new HashMap<>();
        // 内置变量：用户输入
        variables.put("input", input);

        try {
            Object lastOutput = executeNode(startNodeId, nodeMap, outEdges, variables, instance.getId());
            // 工作流成功
            instance.setStatus("success");
            try {
                instance.setOutputJson(objectMapper.writeValueAsString(lastOutput));
            } catch (Exception ignored) {}
            instance.setFinishedAt(LocalDateTime.now());
            instanceMapper.updateById(instance);
        } catch (Exception e) {
            log.error("Workflow execution failed: instanceId={}", instance.getId(), e);
            instance.setStatus("failed");
            instance.setErrorMsg(e.getMessage());
            instance.setFinishedAt(LocalDateTime.now());
            instanceMapper.updateById(instance);
        }

        return instance;
    }

    /**
     * 执行单个节点，返回输出，根据出边决定下一个节点
     */
    private Object executeNode(Long nodeId, Map<Long, WorkflowNodeEntity> nodeMap,
                               Map<Long, List<WorkflowEdgeEntity>> outEdges,
                               Map<String, Object> variables, Long instanceId) {
        WorkflowNodeEntity node = nodeMap.get(nodeId);
        if (node == null) return null;

        // 创建节点执行记录
        NodeExecutionEntity exec = new NodeExecutionEntity();
        exec.setInstanceId(instanceId);
        exec.setNodeId(nodeId);
        exec.setStatus("running");
        exec.setRetryCount(0);
        exec.setStartedAt(LocalDateTime.now());
        try {
            exec.setInputJson(objectMapper.writeValueAsString(variables));
        } catch (Exception ignored) {}
        nodeExecutionMapper.insert(exec);

        // 获取执行器
        NodeExecutor executor = findExecutor(node.getType());
        if (executor == null) {
            exec.setStatus("failed");
            exec.setErrorMsg("未知节点类型: " + node.getType());
            exec.setFinishedAt(LocalDateTime.now());
            nodeExecutionMapper.updateById(exec);
            throw new RuntimeException("未知节点类型: " + node.getType());
        }

        // 执行节点（含重试）
        NodeExecContext ctx = new NodeExecContext();
        ctx.setNode(node);
        ctx.setVariables(variables);

        int maxRetries = getMaxRetries(node);
        NodeExecResult result = null;
        for (int retry = 0; retry <= maxRetries; retry++) {
            result = executor.execute(ctx);
            if (result.isSuccess()) break;
            if (retry < maxRetries) {
                log.warn("Node {} retry {}/{}: {}", nodeId, retry + 1, maxRetries, result.getErrorMsg());
                exec.setRetryCount(retry + 1);
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            }
        }

        // 更新执行记录
        exec.setStatus(result.isSuccess() ? "success" : "failed");
        exec.setErrorMsg(result.getErrorMsg());
        exec.setFinishedAt(LocalDateTime.now());
        try {
            exec.setOutputJson(objectMapper.writeValueAsString(result.getOutput()));
        } catch (Exception ignored) {}
        nodeExecutionMapper.updateById(exec);

        // 失败时检查 error 边
        if (!result.isSuccess()) {
            List<WorkflowEdgeEntity> edges = outEdges.get(nodeId);
            for (WorkflowEdgeEntity edge : edges) {
                if ("error".equals(edge.getEdgeType())) {
                    // 跳转到异常处理节点
                    return executeNode(edge.getTargetNodeId(), nodeMap, outEdges, variables, instanceId);
                }
            }
            throw new RuntimeException("节点 " + node.getName() + " 执行失败: " + result.getErrorMsg());
        }

        // 存储输出到变量
        variables.put(String.valueOf(nodeId), result.getOutput());

        // 根据出边决定下一节点
        List<WorkflowEdgeEntity> edges = outEdges.get(nodeId);
        if (edges.isEmpty()) {
            return result.getOutput(); // 没有出边，结束
        }

        // 条件节点：评估结果决定走 true 还是 false 边
        if ("condition".equals(node.getType()) && result.getOutput() instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> outputMap = (Map<String, Object>) result.getOutput();
            boolean conditionResult = outputMap.get("result") instanceof Boolean
                    ? (Boolean) outputMap.get("result") : false;
            String targetEdgeType = conditionResult ? "true" : "false";
            for (WorkflowEdgeEntity edge : edges) {
                if (targetEdgeType.equals(edge.getEdgeType())) {
                    return executeNode(edge.getTargetNodeId(), nodeMap, outEdges, variables, instanceId);
                }
            }
            // 没有匹配的条件边，结束
            return result.getOutput();
        }

        // 普通节点：取第一条 normal 边
        for (WorkflowEdgeEntity edge : edges) {
            if ("normal".equals(edge.getEdgeType())) {
                return executeNode(edge.getTargetNodeId(), nodeMap, outEdges, variables, instanceId);
            }
        }

        // 没有后继节点，结束
        return result.getOutput();
    }

    private NodeExecutor findExecutor(String type) {
        for (NodeExecutor executor : executors) {
            if (executor.getType().equals(type)) {
                return executor;
            }
        }
        return null;
    }

    private int getMaxRetries(WorkflowNodeEntity node) {
        try {
            Map<String, Object> config = objectMapper.readValue(node.getConfigJson(),
                    new TypeReference<Map<String, Object>>() {});
            if (config != null && config.containsKey("maxRetries")) {
                return ((Number) config.get("maxRetries")).intValue();
            }
        } catch (Exception ignored) {}
        return 0;
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add hify-workflow/src/main/java/com/hify/workflow/engine/WorkflowEngine.java
git commit -m "feat: 添加 DAG 工作流执行引擎"
```

---

### Task 7: WorkflowService

**Files:**
- Modify: `hify-workflow/src/main/java/com/hify/workflow/service/WorkflowService.java`
- Modify: `hify-workflow/src/main/java/com/hify/workflow/service/impl/WorkflowServiceImpl.java`

- [ ] **Step 1: 重写 WorkflowService.java**

```java
package com.hify.workflow.service;

import com.hify.common.result.PageResult;
import com.hify.workflow.dto.*;

public interface WorkflowService {
    Long create(WorkflowCreateReq req);
    void update(Long id, WorkflowUpdateReq req);
    void delete(Long id);
    PageResult<WorkflowResp> list(WorkflowListParams params);
    WorkflowResp getDetail(Long id);
    WorkflowInstanceResp run(Long id, WorkflowRunReq req);
    PageResult<WorkflowInstanceResp> listInstances(Long workflowId, Integer page, Integer pageSize);
    WorkflowInstanceResp getInstanceDetail(Long instanceId);
}
```

- [ ] **Step 2: 重写 WorkflowServiceImpl.java**

```java
package com.hify.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hify.common.exception.BizException;
import com.hify.common.result.PageResult;
import com.hify.common.util.PageHelper;
import com.hify.workflow.dto.*;
import com.hify.workflow.engine.WorkflowEngine;
import com.hify.workflow.entity.*;
import com.hify.workflow.mapper.*;
import com.hify.workflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowServiceImpl implements WorkflowService {

    private final WorkflowMapper workflowMapper;
    private final WorkflowNodeMapper nodeMapper;
    private final WorkflowEdgeMapper edgeMapper;
    private final WorkflowInstanceMapper instanceMapper;
    private final NodeExecutionMapper nodeExecutionMapper;
    private final WorkflowEngine workflowEngine;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public Long create(WorkflowCreateReq req) {
        WorkflowEntity entity = new WorkflowEntity();
        entity.setName(req.getName());
        entity.setDescription(req.getDescription());
        entity.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        workflowMapper.insert(entity);

        List<Long> nodeIds = saveNodes(entity.getId(), req.getNodes());
        saveEdges(entity.getId(), req.getEdges(), nodeIds);

        log.info("Workflow created: id={}, name={}", entity.getId(), entity.getName());
        return entity.getId();
    }

    @Override
    @Transactional
    public void update(Long id, WorkflowUpdateReq req) {
        WorkflowEntity entity = workflowMapper.selectById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw BizException.notFound("工作流不存在");
        }

        if (req.getName() != null) entity.setName(req.getName());
        if (req.getDescription() != null) entity.setDescription(req.getDescription());
        if (req.getStatus() != null) entity.setStatus(req.getStatus());
        workflowMapper.updateById(entity);

        if (req.getNodes() != null) {
            // 删除旧节点和连线
            nodeMapper.delete(new LambdaQueryWrapper<WorkflowNodeEntity>()
                    .eq(WorkflowNodeEntity::getWorkflowId, id));
            edgeMapper.delete(new LambdaQueryWrapper<WorkflowEdgeEntity>()
                    .eq(WorkflowEdgeEntity::getWorkflowId, id));

            List<Long> nodeIds = saveNodes(id, req.getNodes());
            if (req.getEdges() != null) {
                saveEdges(id, req.getEdges(), nodeIds);
            }
        }

        log.info("Workflow updated: id={}, name={}", id, entity.getName());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        WorkflowEntity entity = workflowMapper.selectById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw BizException.notFound("工作流不存在");
        }
        workflowMapper.deleteById(id);
        log.info("Workflow deleted: id={}", id);
    }

    @Override
    public PageResult<WorkflowResp> list(WorkflowListParams params) {
        var page = PageHelper.toPage(params.getPage(), params.getPageSize());
        var wrapper = new LambdaQueryWrapper<WorkflowEntity>()
                .eq(WorkflowEntity::getDeleted, 0)
                .orderByDesc(WorkflowEntity::getCreatedAt);
        if (params.getName() != null && !params.getName().isBlank()) {
            wrapper.like(WorkflowEntity::getName, params.getName());
        }
        if (params.getStatus() != null) {
            wrapper.eq(WorkflowEntity::getStatus, params.getStatus());
        }

        var pageResult = workflowMapper.selectPage(page, wrapper);
        List<WorkflowResp> list = pageResult.getRecords().stream()
                .map(this::toResp)
                .toList();
        return PageHelper.toPageResult(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<WorkflowEntity>(
                        pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal()) {
                    @Override
                    public List<WorkflowEntity> getRecords() {
                        return pageResult.getRecords();
                    }
                });
    }

    @Override
    public WorkflowResp getDetail(Long id) {
        WorkflowEntity entity = workflowMapper.selectById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw BizException.notFound("工作流不存在");
        }
        return toResp(entity);
    }

    @Override
    public WorkflowInstanceResp run(Long id, WorkflowRunReq req) {
        Map<String, Object> input = req.getInput() != null ? req.getInput() : Map.of();
        String triggerType = req.getSessionId() != null ? "agent" : "api";
        WorkflowInstanceEntity instance = workflowEngine.execute(id, input, req.getSessionId(), triggerType);
        return toInstanceResp(instance);
    }

    @Override
    public PageResult<WorkflowInstanceResp> listInstances(Long workflowId, Integer page, Integer pageSize) {
        var pageParam = PageHelper.toPage(page, pageSize);
        var wrapper = new LambdaQueryWrapper<WorkflowInstanceEntity>()
                .eq(workflowId != null, WorkflowInstanceEntity::getWorkflowId, workflowId)
                .orderByDesc(WorkflowInstanceEntity::getCreatedAt);
        var pageResult = instanceMapper.selectPage(pageParam, wrapper);
        List<WorkflowInstanceResp> list = pageResult.getRecords().stream()
                .map(this::toInstanceResp)
                .toList();
        return PageHelper.toPageResult(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<WorkflowInstanceEntity>(
                        pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal()) {
                    @Override
                    public List<WorkflowInstanceEntity> getRecords() {
                        return pageResult.getRecords();
                    }
                });
    }

    @Override
    public WorkflowInstanceResp getInstanceDetail(Long instanceId) {
        WorkflowInstanceEntity instance = instanceMapper.selectById(instanceId);
        if (instance == null) {
            throw BizException.notFound("执行实例不存在");
        }
        WorkflowInstanceResp resp = toInstanceResp(instance);
        List<NodeExecutionEntity> executions = nodeExecutionMapper.selectList(
                new LambdaQueryWrapper<NodeExecutionEntity>()
                        .eq(NodeExecutionEntity::getInstanceId, instanceId)
                        .orderByAsc(NodeExecutionEntity::getCreatedAt));
        resp.setNodeExecutions(executions.stream().map(e -> {
            NodeExecutionResp ner = new NodeExecutionResp();
            BeanUtils.copyProperties(e, ner);
            return ner;
        }).toList());
        return resp;
    }

    // ==================== 私有方法 ====================

    private List<Long> saveNodes(Long workflowId, List<WorkflowDto.NodeItem> nodeItems) {
        List<Long> nodeIds = new ArrayList<>();
        for (WorkflowDto.NodeItem item : nodeItems) {
            WorkflowNodeEntity node = new WorkflowNodeEntity();
            node.setWorkflowId(workflowId);
            node.setName(item.getName());
            node.setType(item.getType());
            node.setConfigJson(item.getConfigJson());
            node.setPositionX(item.getPositionX() != null ? item.getPositionX() : 0);
            node.setPositionY(item.getPositionY() != null ? item.getPositionY() : 0);
            nodeMapper.insert(node);
            nodeIds.add(node.getId());
        }
        return nodeIds;
    }

    private void saveEdges(Long workflowId, List<WorkflowDto.EdgeItem> edgeItems, List<Long> nodeIds) {
        for (WorkflowDto.EdgeItem item : edgeItems) {
            WorkflowEdgeEntity edge = new WorkflowEdgeEntity();
            edge.setWorkflowId(workflowId);
            edge.setSourceNodeId(nodeIds.get(item.getSourceNodeIndex()));
            edge.setTargetNodeId(nodeIds.get(item.getTargetNodeIndex()));
            edge.setEdgeType(item.getEdgeType() != null ? item.getEdgeType() : "normal");
            edge.setConditionExpr(item.getConditionExpr());
            edge.setSortOrder(item.getSortOrder() != null ? item.getSortOrder() : 0);
            edgeMapper.insert(edge);
        }
    }

    private WorkflowResp toResp(WorkflowEntity entity) {
        WorkflowResp resp = new WorkflowResp();
        BeanUtils.copyProperties(entity, resp);

        List<WorkflowNodeEntity> nodes = nodeMapper.selectList(
                new LambdaQueryWrapper<WorkflowNodeEntity>()
                        .eq(WorkflowNodeEntity::getWorkflowId, entity.getId())
                        .eq(WorkflowNodeEntity::getDeleted, 0));
        List<WorkflowDto.NodeItem> nodeItems = nodes.stream().map(n -> {
            WorkflowDto.NodeItem item = new WorkflowDto.NodeItem();
            BeanUtils.copyProperties(n, item);
            return item;
        }).toList();
        resp.setNodes(nodeItems);

        List<WorkflowEdgeEntity> edges = edgeMapper.selectList(
                new LambdaQueryWrapper<WorkflowEdgeEntity>()
                        .eq(WorkflowEdgeEntity::getWorkflowId, entity.getId()));
        // 将 targetNodeId/sourceNodeId 转成 nodeIds 中的索引
        Map<Long, Integer> idToIndex = new java.util.HashMap<>();
        for (int i = 0; i < nodes.size(); i++) {
            idToIndex.put(nodes.get(i).getId(), i);
        }
        List<WorkflowDto.EdgeItem> edgeItems = edges.stream().map(e -> {
            WorkflowDto.EdgeItem item = new WorkflowDto.EdgeItem();
            item.setSourceNodeIndex(idToIndex.get(e.getSourceNodeId()));
            item.setTargetNodeIndex(idToIndex.get(e.getTargetNodeId()));
            item.setEdgeType(e.getEdgeType());
            item.setConditionExpr(e.getConditionExpr());
            item.setSortOrder(e.getSortOrder());
            return item;
        }).toList();
        resp.setEdges(edgeItems);

        return resp;
    }

    private WorkflowInstanceResp toInstanceResp(WorkflowInstanceEntity entity) {
        WorkflowInstanceResp resp = new WorkflowInstanceResp();
        BeanUtils.copyProperties(entity, resp);
        return resp;
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add hify-workflow/src/main/java/com/hify/workflow/service/
git commit -m "feat: 添加 WorkflowService 实现"
```

---

### Task 8: WorkflowController

**Files:**
- Modify: `hify-workflow/src/main/java/com/hify/workflow/controller/WorkflowController.java`

- [ ] **Step 1: 重写 WorkflowController.java**

```java
package com.hify.workflow.controller;

import com.hify.common.result.PageResult;
import com.hify.common.result.Result;
import com.hify.workflow.dto.*;
import com.hify.workflow.service.WorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    @PostMapping
    public Result<Long> create(@Valid @RequestBody WorkflowCreateReq req) {
        return Result.ok(workflowService.create(req));
    }

    @GetMapping
    public Result<PageResult<WorkflowResp>> list(WorkflowListParams params) {
        return Result.ok(workflowService.list(params));
    }

    @GetMapping("/{id}")
    public Result<WorkflowResp> getDetail(@PathVariable Long id) {
        return Result.ok(workflowService.getDetail(id));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody WorkflowUpdateReq req) {
        workflowService.update(id, req);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        workflowService.delete(id);
        return Result.ok();
    }

    @PostMapping("/{id}/run")
    public Result<WorkflowInstanceResp> run(@PathVariable Long id, @RequestBody WorkflowRunReq req) {
        return Result.ok(workflowService.run(id, req));
    }

    @GetMapping("/runs")
    public Result<PageResult<WorkflowInstanceResp>> listInstances(
            @RequestParam(required = false) Long workflowId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.ok(workflowService.listInstances(workflowId, page, pageSize));
    }

    @GetMapping("/runs/{id}")
    public Result<WorkflowInstanceResp> getInstanceDetail(@PathVariable Long id) {
        return Result.ok(workflowService.getInstanceDetail(id));
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add hify-workflow/src/main/java/com/hify/workflow/controller/WorkflowController.java
git commit -m "feat: 添加 WorkflowController REST 接口"
```

---

### Task 9: AgentEntity 添加 workflowId 字段

**Files:**
- Modify: `hify-agent/src/main/java/com/hify/agent/entity/AgentEntity.java`

- [ ] **Step 1: 在 AgentEntity 中添加 workflowId**

在 `private Long kbId;` 之后添加：

```java
/**
 * 绑定工作流 ID
 */
private Long workflowId;
```

- [ ] **Step 2: 提交**

```bash
git add hify-agent/src/main/java/com/hify/agent/entity/AgentEntity.java
git commit -m "feat: Agent 实体新增 workflowId 字段"
```

---

### Task 10: Chat 模块集成工作流触发

**Files:**
- Modify: `hify-chat/src/main/java/com/hify/chat/service/impl/ChatServiceImpl.java`

- [ ] **Step 1: 在 ChatServiceImpl 中添加 WorkflowService 依赖注入**

在已有 final 字段后添加：

```java
private final WorkflowService workflowService;
```

- [ ] **Step 2: 修改 sendMessage 方法，在工作流绑定时分流**

在 `sendMessage` 方法中，`resolveContext` 之后，LLM 调用之前，添加工作流检测逻辑：

```java
// 检查 Agent 是否绑定了工作流
if (ctx.agent.getWorkflowId() != null) {
    Map<String, Object> workflowInput = new java.util.HashMap<>();
    workflowInput.put("user_message", req.getContent());
    workflowInput.put("session_id", sessionId);
    com.hify.workflow.dto.WorkflowRunReq runReq = new com.hify.workflow.dto.WorkflowRunReq();
    runReq.setInput(workflowInput);
    runReq.setSessionId(sessionId);
    com.hify.workflow.dto.WorkflowInstanceResp wfResp =
            workflowService.run(ctx.agent.getWorkflowId(), runReq);

    String wfOutput = wfResp.getOutputJson();
    ChatMessageEntity assistantMsg = new ChatMessageEntity();
    assistantMsg.setSessionId(sessionId);
    assistantMsg.setRole("assistant");
    assistantMsg.setContent(wfOutput != null ? wfOutput : "");
    assistantMsg.setTokenCount(0);
    messageMapper.insert(assistantMsg);
    return toMessageResp(assistantMsg);
}
```

- [ ] **Step 3: sendMessageStream 方法同理**

在保存用户消息之后，解析 context 之后，添加相同的工作流检测逻辑（流式场景下工作流输出整体返回）：

```java
// 检查 Agent 是否绑定了工作流
if (ctx.agent.getWorkflowId() != null) {
    SseEmitter wfEmitter = new SseEmitter(SSE_TIMEOUT_MS);
    Map<String, Object> workflowInput = new java.util.HashMap<>();
    workflowInput.put("user_message", req.getContent());
    workflowInput.put("session_id", sessionId);
    com.hify.workflow.dto.WorkflowRunReq runReq = new com.hify.workflow.dto.WorkflowRunReq();
    runReq.setInput(workflowInput);
    runReq.setSessionId(sessionId);

    // 在独立线程中执行工作流
    executor.execute(() -> {
        try {
            com.hify.workflow.dto.WorkflowInstanceResp wfResp =
                    workflowService.run(ctx.agent.getWorkflowId(), runReq);
            String wfOutput = wfResp.getOutputJson();
            if (wfOutput != null) {
                wfEmitter.send(SseEmitter.event().data(wfOutput));
            }
            wfEmitter.send(SseEmitter.event().data("[DONE]"));
            wfEmitter.complete();
        } catch (Exception e) {
            wfEmitter.completeWithError(e);
        }
    });
    return wfEmitter;
}
```

需要添加 `java.util.concurrent.Executor` 依赖。在类字段中添加：

```java
private final java.util.concurrent.Executor executor = java.util.concurrent.Executors.newCachedThreadPool();
```

- [ ] **Step 4: 提交**

```bash
git add hify-chat/src/main/java/com/hify/chat/service/impl/ChatServiceImpl.java
git commit -m "feat: Chat 模块支持 Agent 绑定工作流自动触发"
```

---

### Task 11: 前端 - Workflow API 层

**Files:**
- Create: `hify-web/src/api/workflow.ts`

- [ ] **Step 1: 创建 workflow.ts**

```typescript
import { get, post, put, del } from '@/utils/request'

// ── 类型定义 ───────────────────────────────────────────

export interface NodeItem {
  name: string
  type: 'llm' | 'condition' | 'rag' | 'http'
  configJson?: string
  positionX?: number
  positionY?: number
}

export interface EdgeItem {
  sourceNodeIndex: number
  targetNodeIndex: number
  edgeType?: 'normal' | 'true' | 'false' | 'error'
  conditionExpr?: string
  sortOrder?: number
}

export interface Workflow {
  id: number
  name: string
  description: string
  status: number
  nodes: NodeItem[]
  edges: EdgeItem[]
  createdAt: string
  updatedAt: string
}

export interface WorkflowCreateReq {
  name: string
  description?: string
  status?: number
  nodes: NodeItem[]
  edges: EdgeItem[]
}

export interface WorkflowUpdateReq {
  name?: string
  description?: string
  status?: number
  nodes?: NodeItem[]
  edges?: EdgeItem[]
}

export interface WorkflowListParams {
  page?: number
  pageSize?: number
  name?: string
  status?: number
}

export interface NodeExecutionResp {
  id: number
  nodeId: number
  nodeName: string
  nodeType: string
  status: string
  inputJson: string
  outputJson: string
  errorMsg: string
  retryCount: number
  startedAt: string
  finishedAt: string
}

export interface WorkflowInstanceResp {
  id: number
  workflowId: number
  workflowName: string
  sessionId: number
  triggerType: string
  status: string
  inputJson: string
  outputJson: string
  errorMsg: string
  startedAt: string
  finishedAt: string
  createdAt: string
  nodeExecutions?: NodeExecutionResp[]
}

export interface WorkflowRunReq {
  input?: Record<string, any>
  sessionId?: number
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
}

// ── API 方法 ───────────────────────────────────────────

export const getWorkflowList = (params?: WorkflowListParams) =>
  get<PageResult<Workflow>>('/v1/workflows', params)

export const createWorkflow = (data: WorkflowCreateReq) =>
  post<number>('/v1/workflows', data)

export const getWorkflowDetail = (id: number) =>
  get<Workflow>(`/v1/workflows/${id}`)

export const updateWorkflow = (id: number, data: WorkflowUpdateReq) =>
  put<void>(`/v1/workflows/${id}`, data)

export const deleteWorkflow = (id: number) =>
  del<void>(`/v1/workflows/${id}`)

export const runWorkflow = (id: number, data?: WorkflowRunReq) =>
  post<WorkflowInstanceResp>(`/v1/workflows/${id}/run`, data)

export const getRunHistory = (workflowId?: number, page?: number, pageSize?: number) =>
  get<PageResult<WorkflowInstanceResp>>('/v1/workflows/runs', { workflowId, page, pageSize })

export const getRunDetail = (id: number) =>
  get<WorkflowInstanceResp>(`/v1/workflows/runs/${id}`)
```

- [ ] **Step 2: 提交**

```bash
git add hify-web/src/api/workflow.ts
git commit -m "feat: 添加工作流前端 API 层"
```

---

### Task 12: 前端 - WorkflowList 页面

**Files:**
- Create: `hify-web/src/views/workflow/WorkflowList.vue`

- [ ] **Step 1: 创建 WorkflowList.vue**

```vue
<template>
  <div class="workflow-list-page">
    <div class="page-toolbar">
      <h2>工作流</h2>
      <el-button type="primary" :icon="Plus" @click="handleCreate">新建工作流</el-button>
    </div>

    <el-table :data="workflows" stripe v-loading="loading">
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="description" label="描述" />
      <el-table-column label="节点数" width="100">
        <template #default="{ row }">
          <el-tag size="small">{{ row.nodes?.length || 0 }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="240">
        <template #default="{ row }">
          <el-button size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button size="small" type="warning" @click="handleRun(row)">运行</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="loadData"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getWorkflowList, deleteWorkflow, runWorkflow, type Workflow } from '@/api/workflow'

const router = useRouter()
const workflows = ref<Workflow[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)

onMounted(() => loadData())

async function loadData() {
  loading.value = true
  try {
    const res = await getWorkflowList({ page: page.value, pageSize: pageSize.value })
    workflows.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleCreate() {
  router.push('/workflows/create')
}

function handleEdit(row: Workflow) {
  router.push(`/workflows/${row.id}/edit`)
}

async function handleRun(row: Workflow) {
  try {
    const res = await runWorkflow(row.id)
    ElMessage.success(`执行完成: ${res.status}`)
    router.push(`/workflows/runs/${res.id}`)
  } catch {
    ElMessage.error('执行失败')
  }
}

async function handleDelete(row: Workflow) {
  await ElMessageBox.confirm('确定删除该工作流吗？', '提示', { type: 'warning' })
  await deleteWorkflow(row.id)
  ElMessage.success('删除成功')
  loadData()
}

function formatDate(s: string) {
  return s ? s.replace('T', ' ').substring(0, 19) : ''
}
</script>

<style scoped>
.workflow-list-page { padding: 0; }
.page-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
```

- [ ] **Step 2: 提交**

```bash
git add hify-web/src/views/workflow/WorkflowList.vue
git commit -m "feat: 添加工作流列表页"
```

---

### Task 13: 前端 - WorkflowEditor 页面

**Files:**
- Create: `hify-web/src/views/workflow/WorkflowEditor.vue`

- [ ] **Step 1: 创建 WorkflowEditor.vue**

```vue
<template>
  <div class="workflow-editor">
    <div class="editor-header">
      <el-button @click="handleBack">返回列表</el-button>
      <h2>{{ isEdit ? '编辑工作流' : '新建工作流' }}</h2>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </div>

    <!-- 基本信息 -->
    <el-card style="margin-bottom: 16px;">
      <template #header>基本信息</template>
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="工作流名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" placeholder="描述" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 节点列表 + 编辑区 -->
    <div class="editor-body">
      <div class="node-list-panel">
        <div class="panel-header">
          <span>节点列表</span>
          <el-button size="small" :icon="Plus" @click="addNode">添加节点</el-button>
        </div>
        <div
          v-for="(node, idx) in form.nodes"
          :key="idx"
          :class="['node-card', { active: selectedNodeIndex === idx }]"
          @click="selectedNodeIndex = idx"
        >
          <span class="node-name">{{ node.name || '未命名' }}</span>
          <el-tag size="small" :type="nodeTypeTag(node.type)">{{ node.type }}</el-tag>
          <el-button size="small" type="danger" circle :icon="Delete" @click.stop="removeNode(idx)" />
        </div>
      </div>

      <div class="node-config-panel" v-if="selectedNodeIndex !== null">
        <el-card>
          <template #header>节点配置</template>
          <el-form :model="currentNode" label-width="100px">
            <el-form-item label="名称">
              <el-input v-model="currentNode.name" />
            </el-form-item>
            <el-form-item label="类型">
              <el-select v-model="currentNode.type">
                <el-option label="LLM 调用" value="llm" />
                <el-option label="条件分支" value="condition" />
                <el-option label="RAG 检索" value="rag" />
                <el-option label="HTTP 请求" value="http" />
              </el-select>
            </el-form-item>

            <!-- LLM 配置 -->
            <template v-if="currentNode.type === 'llm'">
              <el-form-item label="模型配置">
                <el-input v-model="llmConfig.modelConfigId" placeholder="模型配置 ID" />
              </el-form-item>
              <el-form-item label="Prompt">
                <el-input v-model="llmConfig.prompt" type="textarea" :rows="4" placeholder="提示词，支持 {{node_id.field}} 变量" />
              </el-form-item>
              <el-form-item label="最大重试">
                <el-input-number v-model="llmConfig.maxRetries" :min="0" :max="5" />
              </el-form-item>
            </template>

            <!-- RAG 配置 -->
            <template v-if="currentNode.type === 'rag'">
              <el-form-item label="知识库 ID">
                <el-input v-model="ragConfig.kbId" placeholder="知识库 ID" />
              </el-form-item>
              <el-form-item label="查询语句">
                <el-input v-model="ragConfig.query" placeholder="支持 {{input.user_message}} 等变量" />
              </el-form-item>
            </template>

            <!-- HTTP 配置 -->
            <template v-if="currentNode.type === 'http'">
              <el-form-item label="URL">
                <el-input v-model="httpConfig.url" placeholder="https://api.example.com" />
              </el-form-item>
              <el-form-item label="方法">
                <el-select v-model="httpConfig.method">
                  <el-option label="GET" value="GET" />
                  <el-option label="POST" value="POST" />
                  <el-option label="PUT" value="PUT" />
                  <el-option label="DELETE" value="DELETE" />
                </el-select>
              </el-form-item>
              <el-form-item label="Body">
                <el-input v-model="httpConfig.body" type="textarea" :rows="3" />
              </el-form-item>
            </template>

            <!-- 条件配置 -->
            <template v-if="currentNode.type === 'condition'">
              <el-form-item label="表达式">
                <el-input v-model="conditionConfig.expression" placeholder="如 {{node_1.result}} == true" />
              </el-form-item>
            </template>
          </el-form>
        </el-card>

        <!-- 连线配置 -->
        <el-card style="margin-top: 16px;">
          <template #header>连线配置</template>
          <div class="edge-list">
            <div v-for="(edge, idx) in outEdges" :key="idx" class="edge-item">
              <el-select v-model="edge.edgeType" size="small" style="width: 120px;">
                <el-option label="默认" value="normal" />
                <el-option label="条件-真" value="true" />
                <el-option label="条件-假" value="false" />
                <el-option label="异常" value="error" />
              </el-select>
              <el-select v-model="edge.targetNodeIndex" size="small" style="width: 160px;">
                <el-option
                  v-for="(n, i) in form.nodes"
                  :key="i"
                  :label="n.name || `节点 ${i + 1}`"
                  :value="i"
                />
              </el-select>
              <el-input v-if="edge.edgeType === 'true' || edge.edgeType === 'false'"
                v-model="edge.conditionExpr" size="small" placeholder="条件表达式" style="width: 160px;" />
              <el-button size="small" type="danger" circle :icon="Delete" @click="removeEdge(idx)" />
            </div>
            <el-button size="small" :icon="Plus" @click="addEdge">添加连线</el-button>
          </div>
        </el-card>
      </div>

      <div v-else class="empty-hint">点击左侧节点进行配置</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Plus, Delete } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  createWorkflow, getWorkflowDetail, updateWorkflow,
  type NodeItem, type EdgeItem
} from '@/api/workflow'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => !!route.params.id)

const form = ref({ name: '', description: '', status: 1, nodes: [] as NodeItem[], edges: [] as EdgeItem[] })
const selectedNodeIndex = ref<number | null>(null)

const currentNode = computed(() => {
  if (selectedNodeIndex.value === null || !form.value.nodes[selectedNodeIndex.value]) {
    return {} as NodeItem
  }
  return form.value.nodes[selectedNodeIndex.value]
})

function parseConfig() {
  if (!currentNode.value.configJson) return {}
  try { return JSON.parse(currentNode.value.configJson) } catch { return {} }
}

const llmConfig = computed({
  get: () => parseConfig(),
  set: (val) => updateConfig(val)
})
const ragConfig = computed({
  get: () => parseConfig(),
  set: (val) => updateConfig(val)
})
const httpConfig = computed({
  get: () => parseConfig(),
  set: (val) => updateConfig(val)
})
const conditionConfig = computed({
  get: () => parseConfig(),
  set: (val) => updateConfig(val)
})

function updateConfig(val: any) {
  if (selectedNodeIndex.value !== null) {
    form.value.nodes[selectedNodeIndex.value].configJson = JSON.stringify(val)
  }
}

const outEdges = computed(() => {
  if (selectedNodeIndex.value === null) return []
  return form.value.edges.filter(e => e.sourceNodeIndex === selectedNodeIndex.value)
})

function addNode() {
  form.value.nodes.push({ name: '新节点', type: 'llm', configJson: '{}', positionX: 0, positionY: 0 })
}

function removeNode(idx: number) {
  form.value.nodes.splice(idx, 1)
  form.value.edges = form.value.edges.filter(e => e.sourceNodeIndex !== idx && e.targetNodeIndex !== idx)
  if (selectedNodeIndex.value === idx) selectedNodeIndex.value = null
}

function addEdge() {
  if (selectedNodeIndex.value === null) return
  form.value.edges.push({ sourceNodeIndex: selectedNodeIndex.value, targetNodeIndex: 0, edgeType: 'normal' })
}

function removeEdge(idx: number) {
  const globalIdx = form.value.edges.indexOf(outEdges.value[idx])
  if (globalIdx >= 0) form.value.edges.splice(globalIdx, 1)
}

function nodeTypeTag(type: string) {
  const map: Record<string, string> = { llm: 'primary', condition: 'warning', rag: 'success', http: 'info' }
  return map[type] || ''
}

async function handleSave() {
  try {
    if (isEdit.value) {
      await updateWorkflow(Number(route.params.id), form.value)
    } else {
      await createWorkflow(form.value)
    }
    ElMessage.success('保存成功')
    router.push('/workflows')
  } catch {
    ElMessage.error('保存失败')
  }
}

function handleBack() {
  router.push('/workflows')
}

onMounted(async () => {
  if (isEdit.value) {
    const detail = await getWorkflowDetail(Number(route.params.id))
    form.value = {
      name: detail.name,
      description: detail.description || '',
      status: detail.status,
      nodes: detail.nodes || [],
      edges: detail.edges || []
    }
  }
})
</script>

<style scoped>
.workflow-editor { padding: 0; }
.editor-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.editor-body { display: grid; grid-template-columns: 280px 1fr; gap: 16px; }
.node-list-panel { border: 1px solid var(--el-border-color); border-radius: 8px; padding: 12px; }
.panel-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.node-card { display: flex; align-items: center; gap: 8px; padding: 8px; border-radius: 6px; cursor: pointer; margin-bottom: 4px; }
.node-card:hover { background: var(--el-fill-color-light); }
.node-card.active { background: var(--el-color-primary-light-9); border: 1px solid var(--el-color-primary); }
.node-name { flex: 1; font-size: 14px; }
.node-config-panel { min-height: 400px; }
.empty-hint { display: flex; align-items: center; justify-content: center; color: var(--el-text-color-secondary); min-height: 400px; }
.edge-list { display: flex; flex-direction: column; gap: 8px; }
.edge-item { display: flex; gap: 8px; align-items: center; }
</style>
```

- [ ] **Step 2: 提交**

```bash
git add hify-web/src/views/workflow/WorkflowEditor.vue
git commit -m "feat: 添加工作流编辑器页面"
```

---

### Task 14: 前端 - 路由和菜单更新

**Files:**
- Modify: `hify-web/src/router/index.ts`
- Modify: `hify-web/src/views/DashboardLayout.vue`

- [ ] **Step 1: router/index.ts 新增工作流路由**

在 routes 数组末尾（`]` 之前）添加：

```typescript
  {
    path: '/workflows',
    name: 'WorkflowList',
    component: () => import('@/views/workflow/WorkflowList.vue'),
    meta: { title: '工作流' }
  },
  {
    path: '/workflows/create',
    name: 'WorkflowCreate',
    component: () => import('@/views/workflow/WorkflowEditor.vue'),
    meta: { title: '新建工作流' }
  },
  {
    path: '/workflows/:id/edit',
    name: 'WorkflowEdit',
    component: () => import('@/views/workflow/WorkflowEditor.vue'),
    meta: { title: '编辑工作流' }
  }
```

- [ ] **Step 2: DashboardLayout.vue 侧边菜单新增"工作流"项**

在 `menuItems` 数组中，`{ path: '/chat', label: '对话', icon: ChatDotRound }` 之后添加：

```typescript
  { path: '/workflows', label: '工作流', icon: Share },
```

在 import 中添加 `Share` 图标（来自 `@element-plus/icons-vue`）：

```typescript
import {
  HomeFilled, Cpu, ChatDotRound, Folder, Setting,
  Plus, Search, Upload, Document, DataLine, Monitor, Share
} from '@element-plus/icons-vue'
```

- [ ] **Step 3: 提交**

```bash
git add hify-web/src/router/index.ts hify-web/src/views/DashboardLayout.vue
git commit -m "feat: 前端新增工作流路由和菜单项"
```

---

## 自检清单

**Spec 覆盖:**
- [x] 数据模型 5 张表 → Task 1
- [x] Workflow CRUD API → Task 7, 8
- [x] Workflow 执行 API → Task 7, 8
- [x] 四种节点执行器 → Task 5
- [x] DAG 调度引擎 → Task 6
- [x] 变量传递 → Task 5, 6
- [x] 重试 + 异常分支 → Task 6
- [x] Agent 绑定工作流 → Task 9, 10
- [x] Chat 模块集成 → Task 10
- [x] 前端列表页 → Task 12
- [x] 前端编辑器 → Task 13
- [x] 路由 + 菜单 → Task 14

**无占位符:** 确认通过

**类型一致性:** DTO 字段名与实体字段对齐，前端类型与后端 API 对齐
