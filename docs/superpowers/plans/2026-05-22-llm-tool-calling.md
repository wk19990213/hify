# LLM 节点 MCP 工具调用 — 实现计划

> **For agentic workers:** 使用 subagent-driven-development 逐任务实现。步骤使用 checkbox (`- [ ]`) 语法跟踪进度。

**Goal:** 工作流中 AI 对话节点支持 LLM 自主调用 MCP 工具，替代硬编码 HTTP 调用。

**Architecture:** 自建精简 MCP 客户端（JSON-RPC over stdio/SSE）→ ProviderAdapter 扩展 tools 参数 → LlmNodeExecutor 增加 3 轮工具调用循环 → ChatServiceImpl 解析 Agent 工具绑定并传入 WorkflowRunReq → WorkflowEngine 透传到 NodeExecContext → 前端 LLM 节点配置增加工具勾选。

**Tech Stack:** Java 17+ stdlib (Process, HttpURLConnection), Spring Boot, MyBatis-Plus, Vue 3, Element Plus

**依赖变更:**
- `hify-provider` 新增依赖 `hify-mcp`（ChatRequest 引用 ToolDef）
- `hify-workflow` 新增依赖 `hify-mcp`（NodeExecContext / LlmNodeExecutor 引用 ToolDef、McpClientManager）

---

### Task 1: MCP 传输层 — 接口与 StdioTransport

**Files:**
- Create: `hify-mcp/src/main/java/com/hify/mcp/mcp/McpTransport.java`
- Create: `hify-mcp/src/main/java/com/hify/mcp/mcp/StdioTransport.java`

- [ ] **Step 1: 创建传输接口**

```java
// McpTransport.java
package com.hify.mcp.mcp;

import com.fasterxml.jackson.databind.JsonNode;

public interface McpTransport extends AutoCloseable {
    /** 发送 JSON-RPC 请求，返回响应 */
    JsonNode send(String method, JsonNode params) throws Exception;
}
```

- [ ] **Step 2: 实现 StdioTransport**

```java
// StdioTransport.java
package com.hify.mcp.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class StdioTransport implements McpTransport {
    private final List<String> command;
    private Process process;
    private BufferedWriter writer;
    private BufferedReader reader;
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicInteger requestId = new AtomicInteger(1);

    public StdioTransport(String command, List<String> args) {
        this.command = new java.util.ArrayList<>();
        this.command.add(command);
        if (args != null) this.command.addAll(args);
    }

    @Override
    public JsonNode send(String method, JsonNode params) throws Exception {
        ensureProcess();
        ObjectNode request = mapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("method", method);
        request.put("id", requestId.getAndIncrement());
        if (params != null) request.set("params", params);

        String reqJson = mapper.writeValueAsString(request);
        log.debug("MCP >> {}", reqJson);
        writer.write(reqJson);
        writer.newLine();
        writer.flush();

        String line = reader.readLine();
        if (line == null) throw new IOException("MCP process closed unexpectedly");
        log.debug("MCP << {}", line);
        JsonNode resp = mapper.readTree(line);
        if (resp.has("error")) {
            throw new IOException("MCP error: " + resp.get("error"));
        }
        return resp.get("result");
    }

    private void ensureProcess() throws IOException {
        if (process != null && process.isAlive()) return;
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(false);
        process = pb.start();
        writer = new BufferedWriter(new OutputStreamWriter(
                process.getOutputStream(), StandardCharsets.UTF_8));
        reader = new BufferedReader(new InputStreamReader(
                process.getInputStream(), StandardCharsets.UTF_8));
    }

    @Override
    public void close() {
        if (process != null) {
            process.destroy();
            try { process.waitFor(); } catch (InterruptedException ignored) {}
        }
    }
}
```

- [ ] **Step 3: 编译验证**

```bash
cd C:/Users/11953/Desktop/hify && ./mvnw compile -pl hify-mcp -am -q
```

- [ ] **Step 4: 提交**

```bash
git add hify-mcp/src/main/java/com/hify/mcp/mcp/
git commit -m "feat: 新增 MCP 传输接口和 StdioTransport 实现"
```

---

### Task 2: SseTransport 实现

**Files:**
- Create: `hify-mcp/src/main/java/com/hify/mcp/mcp/SseTransport.java`

- [ ] **Step 1: 实现 SseTransport**

```java
// SseTransport.java
package com.hify.mcp.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class SseTransport implements McpTransport {
    private final String url;
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicInteger requestId = new AtomicInteger(1);

    public SseTransport(String url) {
        this.url = url;
    }

    @Override
    public JsonNode send(String method, JsonNode params) throws Exception {
        ObjectNode request = mapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("method", method);
        request.put("id", requestId.getAndIncrement());
        if (params != null) request.set("params", params);

        byte[] body = mapper.writeValueAsBytes(request);
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body);
        }

        int status = conn.getResponseCode();
        InputStream is = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
        String respBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        JsonNode resp = mapper.readTree(respBody);
        if (resp.has("error")) {
            throw new IOException("MCP error: " + resp.get("error"));
        }
        return resp.get("result");
    }

    @Override
    public void close() {
        // SSE transport is stateless, nothing to close
    }
}
```

- [ ] **Step 2: 编译验证并提交**

```bash
cd C:/Users/11953/Desktop/hify && ./mvnw compile -pl hify-mcp -am -q
git add hify-mcp/src/main/java/com/hify/mcp/mcp/SseTransport.java
git commit -m "feat: 新增 SseTransport MCP 传输实现"
```

---

### Task 3: MCP 客户端与工具模型

**Files:**
- Create: `hify-mcp/src/main/java/com/hify/mcp/mcp/ToolDef.java`
- Create: `hify-mcp/src/main/java/com/hify/mcp/mcp/ToolResult.java`
- Create: `hify-mcp/src/main/java/com/hify/mcp/mcp/McpClient.java`
- Create: `hify-mcp/src/main/java/com/hify/mcp/mcp/McpClientManager.java`

- [ ] **Step 1: 工具定义和结果模型**

```java
// ToolDef.java
package com.hify.mcp.mcp;

import lombok.Data;
import java.util.Map;

@Data
public class ToolDef {
    private String name;
    private String description;
    private Map<String, Object> inputSchema;  // JSON Schema
    private Long serverId;                     // 来源 MCP 服务器 ID
}
```

```java
// ToolResult.java
package com.hify.mcp.mcp;

import lombok.Data;

@Data
public class ToolResult {
    private boolean success;
    private String content;    // 工具返回的文本内容
    private String error;      // 错误信息（success=false 时）
}
```

- [ ] **Step 2: McpClient — JSON-RPC 编解码**

```java
// McpClient.java
package com.hify.mcp.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class McpClient implements AutoCloseable {
    private final McpTransport transport;
    private final ObjectMapper mapper = new ObjectMapper();

    public McpClient(McpTransport transport) {
        this.transport = transport;
    }

    public List<ToolDef> listTools() throws Exception {
        JsonNode result = transport.send("tools/list", null);
        List<ToolDef> tools = new ArrayList<>();
        JsonNode toolsNode = result.get("tools");
        if (toolsNode != null && toolsNode.isArray()) {
            for (JsonNode t : toolsNode) {
                ToolDef def = new ToolDef();
                def.setName(t.get("name").asText());
                def.setDescription(t.has("description")
                        ? t.get("description").asText() : "");
                if (t.has("inputSchema")) {
                    def.setInputSchema(mapper.convertValue(
                            t.get("inputSchema"), Map.class));
                }
                tools.add(def);
            }
        }
        return tools;
    }

    public ToolResult callTool(String name, Map<String, Object> arguments) {
        ToolResult result = new ToolResult();
        try {
            ObjectNode params = mapper.createObjectNode();
            params.put("name", name);
            params.set("arguments", mapper.valueToTree(
                    arguments != null ? arguments : Map.of()));
            JsonNode resp = transport.send("tools/call", params);
            JsonNode content = resp.get("content");
            if (content != null && content.isArray()) {
                StringBuilder sb = new StringBuilder();
                for (JsonNode c : content) {
                    String type = c.has("type") ? c.get("type").asText() : "text";
                    if ("text".equals(type)) {
                        sb.append(c.has("text") ? c.get("text").asText() : "");
                    }
                }
                result.setContent(sb.toString());
            }
            result.setSuccess(true);
        } catch (Exception e) {
            log.error("MCP tool call failed: {}", name, e);
            result.setSuccess(false);
            result.setError(e.getMessage());
        }
        return result;
    }

    @Override
    public void close() {
        transport.close();
    }
}
```

- [ ] **Step 3: McpClientManager — 客户端生命周期**

```java
// McpClientManager.java
package com.hify.mcp.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hify.mcp.entity.McpServerEntity;
import com.hify.mcp.mapper.McpServerMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class McpClientManager {
    private final McpServerMapper serverMapper;
    private final ObjectMapper objectMapper;
    private final Map<Long, McpClient> clients = new ConcurrentHashMap<>();

    public List<ToolDef> listTools(Long serverId) {
        try {
            McpClient client = getOrCreate(serverId);
            List<ToolDef> tools = client.listTools();
            for (ToolDef t : tools) t.setServerId(serverId);
            return tools;
        } catch (Exception e) {
            log.error("Failed to list tools for MCP server {}", serverId, e);
            return List.of();
        }
    }

    public ToolResult callTool(Long serverId, String name,
                               Map<String, Object> arguments) {
        try {
            McpClient client = getOrCreate(serverId);
            return client.callTool(name, arguments);
        } catch (Exception e) {
            log.error("Failed to call tool {} on server {}", name, serverId, e);
            ToolResult r = new ToolResult();
            r.setSuccess(false);
            r.setError(e.getMessage());
            return r;
        }
    }

    private McpClient getOrCreate(Long serverId) throws Exception {
        return clients.computeIfAbsent(serverId, id -> {
            McpServerEntity server = serverMapper.selectById(id);
            if (server == null)
                throw new RuntimeException("MCP server not found: " + id);
            McpTransport transport = createTransport(server);
            return new McpClient(transport);
        });
    }

    private McpTransport createTransport(McpServerEntity server) {
        if ("sse".equals(server.getTransportType())) {
            return new SseTransport(server.getUrl());
        }
        List<String> args = parseArgs(server.getArgsJson());
        return new StdioTransport(server.getCommand(), args);
    }

    @SuppressWarnings("unchecked")
    private List<String> parseArgs(String argsJson) {
        if (argsJson == null || argsJson.isBlank()) return List.of();
        try {
            return objectMapper.readValue(argsJson, List.class);
        } catch (Exception e) {
            return List.of(argsJson);
        }
    }

    public void evict(Long serverId) {
        McpClient client = clients.remove(serverId);
        if (client != null) client.close();
    }
}
```

- [ ] **Step 4: 编译验证并提交**

```bash
cd C:/Users/11953/Desktop/hify && ./mvnw compile -pl hify-mcp -am -q
git add hify-mcp/src/main/java/com/hify/mcp/mcp/
git commit -m "feat: 新增 McpClient 和 McpClientManager 工具发现和调用"
```

---

### Task 4: ChatRequest 扩展 + ProviderAdapter 工具支持

**Files:**
- Modify: `hify-provider/src/main/java/com/hify/provider/adapter/ChatRequest.java`
- Modify: `hify-provider/src/main/java/com/hify/provider/adapter/ProviderAdapter.java`
- Modify: `hify-provider/src/main/java/com/hify/provider/adapter/AbstractProviderAdapter.java`
- Modify: `hify-provider/src/main/java/com/hify/provider/adapter/OpenAiAdapter.java`
- Modify: `hify-provider/src/main/java/com/hify/provider/adapter/AnthropicAdapter.java`
- Modify: `hify-provider/pom.xml`（新增 hify-mcp 依赖）

- [ ] **Step 1: pom.xml 新增 hify-mcp 依赖**

```xml
<!-- 在 hify-provider/pom.xml 的 dependencies 中添加 -->
<dependency>
    <groupId>com.hify</groupId>
    <artifactId>hify-mcp</artifactId>
    <version>${project.version}</version>
</dependency>
```

- [ ] **Step 2: ChatRequest 新增 tools 参数**

```java
// ChatRequest.java — 完整替换
package com.hify.provider.adapter;

import com.hify.mcp.mcp.ToolDef;
import java.util.List;
import java.util.Map;

public record ChatRequest(
    String model,
    List<Map<String, String>> messages,
    double temperature,
    boolean stream,
    List<ToolDef> tools
) {
    /** 向后兼容：无 tools 的便捷构造器 */
    public ChatRequest(String model, List<Map<String, String>> messages,
                       double temperature, boolean stream) {
        this(model, messages, temperature, stream, null);
    }
}
```

- [ ] **Step 3: ProviderAdapter 新增 extractToolCalls 方法和 ToolCall 内部类**

Modify: `hify-provider/src/main/java/com/hify/provider/adapter/ProviderAdapter.java`

在接口末尾（`listModelIds` 之后）新增：

```java
/** 从同步响应中提取工具调用列表 */
List<ToolCall> extractToolCalls(String responseBody);

/** LLM 返回的工具调用 */
@Data
class ToolCall {
    private String id;
    private String name;
    private Map<String, Object> arguments;
}
```

注意：`ToolCall` 是 ProviderAdapter 接口的内部类，引用时写作 `ProviderAdapter.ToolCall`。

- [ ] **Step 4: AbstractProviderAdapter 新增工具格式化**

Modify: `hify-provider/src/main/java/com/hify/provider/adapter/AbstractProviderAdapter.java`

在 `buildOpenAiBody()` 方法中，`body.put("stream", request.stream());` 之后添加：

```java
if (request.tools() != null && !request.tools().isEmpty()) {
    body.put("tools", formatTools(request.tools()));
}
```

在类末尾新增 `formatTools` 方法：

```java
/** 将 ToolDef 列表转换为 OpenAI 工具格式，AnthropicAdapter 可覆盖 */
protected List<Map<String, Object>> formatTools(List<ToolDef> tools) {
    List<Map<String, Object>> result = new ArrayList<>();
    for (ToolDef t : tools) {
        Map<String, Object> func = new LinkedHashMap<>();
        func.put("name", t.getName());
        func.put("description", t.getDescription());
        if (t.getInputSchema() != null) {
            func.put("parameters", t.getInputSchema());
        }
        result.add(Map.of("type", "function", "function", func));
    }
    return result;
}
```

- [ ] **Step 5: OpenAiAdapter 实现 extractToolCalls**

Modify: `hify-provider/src/main/java/com/hify/provider/adapter/OpenAiAdapter.java`

在类末尾添加：

```java
@Override
public List<ToolCall> extractToolCalls(String responseBody) {
    List<ToolCall> calls = new ArrayList<>();
    try {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode choices = root.get("choices");
        if (choices != null && choices.isArray() && choices.size() > 0) {
            JsonNode message = choices.get(0).get("message");
            JsonNode toolCalls = message.get("tool_calls");
            if (toolCalls != null && toolCalls.isArray()) {
                for (JsonNode tc : toolCalls) {
                    ToolCall call = new ToolCall();
                    call.setId(tc.get("id").asText());
                    JsonNode func = tc.get("function");
                    call.setName(func.get("name").asText());
                    String argsJson = func.get("arguments").asText();
                    call.setArguments(objectMapper.readValue(argsJson, Map.class));
                    calls.add(call);
                }
            }
        }
    } catch (Exception e) {
        log.warn("Failed to extract OpenAI tool calls", e);
    }
    return calls;
}
```

- [ ] **Step 6: AnthropicAdapter 覆盖工具格式和 extractToolCalls**

Modify: `hify-provider/src/main/java/com/hify/provider/adapter/AnthropicAdapter.java`

在 `buildOpenAiBody()` 方法中，`body.put("stream", request.stream());` 之后添加：

```java
if (request.tools() != null && !request.tools().isEmpty()) {
    body.put("tools", formatTools(request.tools()));
}
```

在类末尾添加：

```java
@Override
protected List<Map<String, Object>> formatTools(List<ToolDef> tools) {
    List<Map<String, Object>> result = new ArrayList<>();
    for (ToolDef t : tools) {
        Map<String, Object> tool = new LinkedHashMap<>();
        tool.put("name", t.getName());
        tool.put("description", t.getDescription());
        tool.put("input_schema", t.getInputSchema() != null
                ? t.getInputSchema() : Map.of("type", "object"));
        result.add(tool);
    }
    return result;
}

@Override
public List<ToolCall> extractToolCalls(String responseBody) {
    List<ToolCall> calls = new ArrayList<>();
    try {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode content = root.get("content");
        if (content != null && content.isArray()) {
            for (JsonNode block : content) {
                if ("tool_use".equals(block.get("type").asText())) {
                    ToolCall call = new ToolCall();
                    call.setId(block.get("id").asText());
                    call.setName(block.get("name").asText());
                    call.setArguments(objectMapper.convertValue(
                            block.get("input"), Map.class));
                    calls.add(call);
                }
            }
        }
    } catch (Exception e) {
        log.warn("Failed to extract Anthropic tool calls", e);
    }
    return calls;
}
```

- [ ] **Step 7: 编译验证并提交**

```bash
cd C:/Users/11953/Desktop/hify && ./mvnw compile -pl hify-provider -am -q
git add hify-provider/
git commit -m "feat: ChatRequest 新增 tools，ProviderAdapter 支持工具调用格式"
```

---

### Task 5: LlmNodeExecutor 工具调用循环

**Files:**
- Modify: `hify-workflow/pom.xml`（新增 hify-mcp 依赖）
- Modify: `hify-workflow/src/main/java/com/hify/workflow/engine/NodeExecContext.java`
- Modify: `hify-workflow/src/main/java/com/hify/workflow/engine/LlmNodeExecutor.java`

- [ ] **Step 1: pom.xml 新增 hify-mcp 依赖**

```xml
<!-- 在 hify-workflow/pom.xml 的 dependencies 中添加 -->
<dependency>
    <groupId>com.hify</groupId>
    <artifactId>hify-mcp</artifactId>
    <version>${project.version}</version>
</dependency>
```

- [ ] **Step 2: NodeExecContext 新增 tools 字段**

Modify: `hify-workflow/src/main/java/com/hify/workflow/engine/NodeExecContext.java`

```java
// 在 modelConfigId 字段之后新增：
/** 可用的 MCP 工具列表，由调用方传入，LLM 节点使用 */
private List<ToolDef> tools;
```

同时新增 import：`import com.hify.mcp.mcp.ToolDef;`

- [ ] **Step 3: LlmNodeExecutor 注入 McpClientManager 并实现工具调用循环**

Modify: `hify-workflow/src/main/java/com/hify/workflow/engine/LlmNodeExecutor.java`

新增依赖注入：

```java
// 在已有 final 字段后新增：
private final McpClientManager mcpClientManager;
```

新增 import：
```java
import com.hify.mcp.mcp.McpClientManager;
import com.hify.mcp.mcp.ToolDef;
import com.hify.mcp.mcp.ToolResult;
```

完全重写 `execute()` 方法：

```java
@Override
public NodeExecResult execute(NodeExecContext ctx) {
    String configJson = ctx.getNode().getConfigJson();
    Map<String, Object> config;
    try {
        config = objectMapper.readValue(configJson,
                new TypeReference<Map<String, Object>>() {});
    } catch (Exception e) {
        return NodeExecResult.builder().success(false)
                .errorMsg("LLM 节点配置解析失败: " + e.getMessage()).build();
    }

    Long modelConfigId = ctx.getModelConfigId();
    String prompt = (String) config.get("prompt");

    if (modelConfigId == null) {
        return NodeExecResult.builder().success(false)
                .errorMsg("LLM 节点缺少模型配置，请在工作流绑定的 Agent 中配置模型").build();
    }
    if (prompt == null) {
        return NodeExecResult.builder().success(false)
                .errorMsg("LLM 节点缺少 Prompt").build();
    }

    prompt = resolveVariables(prompt, ctx.getVariables());

    try {
        ModelConfigEntity modelConfig = modelConfigMapper.selectById(modelConfigId);
        if (modelConfig == null || modelConfig.getDeleted() == 1) {
            return NodeExecResult.builder().success(false)
                    .errorMsg("模型配置不存在").build();
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
            return NodeExecResult.builder().success(false)
                    .errorMsg("没有可用的模型提供商").build();
        }

        ProviderAdapter adapter = adapterFactory.getAdapter(provider.getType());

        String authJson = null;
        String encrypted = provider.getAuthConfig();
        if (encrypted != null && !encrypted.isEmpty()) {
            authJson = AesEncryptor.decrypt(encrypted);
        }
        Map<String, Object> authConfig = objectMapper.readValue(authJson,
                new TypeReference<Map<String, Object>>() {});

        // 构建初始 messages
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "user", "content", prompt));

        // 从 context 获取工具列表（按节点配置过滤）
        List<ToolDef> tools = resolveNodeTools(config, ctx.getTools());
        String lastContent = null;
        String lastResponse = null;

        for (int round = 1; round <= 3; round++) {
            ChatRequest chatReq = new ChatRequest(
                    modelConfig.getModelId(), messages, 0.7, false, tools);
            lastResponse = adapter.chat(provider.getBaseUrl(), authConfig, chatReq);
            lastContent = adapter.extractContent(lastResponse);
            List<ProviderAdapter.ToolCall> toolCalls =
                    adapter.extractToolCalls(lastResponse);

            if (toolCalls.isEmpty()) {
                break;  // LLM 直接回复，循环结束
            }

            // 执行工具调用
            for (ProviderAdapter.ToolCall tc : toolCalls) {
                ToolResult tr = executeToolCall(tc, tools);
                messages.add(Map.of("role", "assistant", "content",
                        toToolCallJson(tc)));
                messages.add(Map.of("role", "tool", "content",
                        tr.isSuccess() ? tr.getContent()
                                : "Error: " + tr.getError()));
            }

            if (lastContent != null && !lastContent.isBlank()) {
                break;  // LLM 同时给出回复和工具调用，取回复
            }
        }

        // 构建输出
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("content", lastContent != null ? lastContent : "");
        String jsonStr = extractJson(lastContent);
        if (jsonStr != null) {
            try {
                Map<String, Object> parsed = objectMapper.readValue(jsonStr,
                        new TypeReference<Map<String, Object>>() {});
                output.putAll(parsed);
            } catch (Exception ignored) {}
        }

        return NodeExecResult.builder().success(true).output(output).build();
    } catch (Exception e) {
        log.error("LLM node execution failed: nodeId={}", ctx.getNode().getId(), e);
        return NodeExecResult.builder().success(false)
                .errorMsg("LLM 调用失败: " + e.getMessage()).build();
    }
}
```

新增辅助方法：

```java
/** 按节点配置过滤工具列表（配置中 toolsEnabled=true 且 tools 列出名称） */
private List<ToolDef> resolveNodeTools(Map<String, Object> config,
                                        List<ToolDef> allTools) {
    if (allTools == null || allTools.isEmpty()) return null;
    Boolean enabled = (Boolean) config.get("toolsEnabled");
    if (!Boolean.TRUE.equals(enabled)) return null;
    @SuppressWarnings("unchecked")
    List<String> selectedNames = (List<String>) config.get("tools");
    if (selectedNames == null || selectedNames.isEmpty()) return null;
    return allTools.stream()
            .filter(t -> selectedNames.contains(t.getName()))
            .toList();
}

private String toToolCallJson(ProviderAdapter.ToolCall tc) {
    try {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", tc.getName());
        m.put("arguments", tc.getArguments());
        return objectMapper.writeValueAsString(m);
    } catch (Exception e) {
        return "{}";
    }
}

private ToolResult executeToolCall(ProviderAdapter.ToolCall tc,
                                    List<ToolDef> tools) {
    if (tools == null || tools.isEmpty()) {
        ToolResult r = new ToolResult();
        r.setSuccess(false);
        r.setError("没有可用的工具");
        return r;
    }
    ToolDef def = tools.stream()
            .filter(t -> t.getName().equals(tc.getName()))
            .findFirst().orElse(null);
    if (def == null || def.getServerId() == null) {
        ToolResult r = new ToolResult();
        r.setSuccess(false);
        r.setError("未找到工具: " + tc.getName());
        return r;
    }
    return mcpClientManager.callTool(def.getServerId(), tc.getName(),
            tc.getArguments());
}
```

- [ ] **Step 4: 编译验证并提交**

```bash
cd C:/Users/11953/Desktop/hify && ./mvnw compile -pl hify-workflow -am -q
git add hify-workflow/
git commit -m "feat: LlmNodeExecutor 实现 3 轮工具调用循环"
```

---

### Task 6: ChatServiceImpl 解析工具 + WorkflowEngine 透传

**Files:**
- Modify: `hify-workflow/src/main/java/com/hify/workflow/dto/WorkflowRunReq.java`
- Modify: `hify-workflow/src/main/java/com/hify/workflow/engine/WorkflowEngine.java`
- Modify: `hify-workflow/src/main/java/com/hify/workflow/service/impl/WorkflowServiceImpl.java`
- Modify: `hify-chat/src/main/java/com/hify/chat/service/impl/ChatServiceImpl.java`

- [ ] **Step 1: WorkflowRunReq 新增 tools 字段**

Modify: `hify-workflow/src/main/java/com/hify/workflow/dto/WorkflowRunReq.java`

```java
package com.hify.workflow.dto;

import com.hify.mcp.mcp.ToolDef;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class WorkflowRunReq {
    private Map<String, Object> input;
    private Long sessionId;
    /** 模型配置 ID，由调用方（如 Agent）传入，LLM 节点将使用此模型 */
    private Long modelConfigId;
    /** MCP 工具列表，由 ChatServiceImpl 解析 Agent 工具绑定后传入 */
    private List<ToolDef> tools;
}
```

- [ ] **Step 2: WorkflowEngine.execute() 新增 tools 参数**

Modify: `hify-workflow/src/main/java/com/hify/workflow/engine/WorkflowEngine.java`

方法签名从：
```java
public WorkflowInstanceEntity execute(Long workflowId, Map<String, Object> input,
        Long sessionId, String triggerType, Long modelConfigId) {
```
改为：
```java
public WorkflowInstanceEntity execute(Long workflowId, Map<String, Object> input,
        Long sessionId, String triggerType, Long modelConfigId,
        List<ToolDef> tools) {
```

在 `executeNode` 调用处（`executeNode(startNodeId, ...)` 和后续递归调用），新增 `tools` 参数：

```java
// 在 execute() 中：
Object lastOutput = executeNode(startNodeId, nodeMap, outEdges, variables,
        instance.getId(), modelConfigId, tools);
```

方法签名从：
```java
private Object executeNode(Long nodeId, Map<Long, WorkflowNodeEntity> nodeMap,
        Map<Long, List<WorkflowEdgeEntity>> outEdges,
        Map<String, Object> variables, Long instanceId, Long modelConfigId) {
```
改为：
```java
private Object executeNode(Long nodeId, Map<Long, WorkflowNodeEntity> nodeMap,
        Map<Long, List<WorkflowEdgeEntity>> outEdges,
        Map<String, Object> variables, Long instanceId, Long modelConfigId,
        List<ToolDef> tools) {
```

在 `executeNode()` 中设置 context：
```java
// 在 ctx.setModelConfigId(modelConfigId); 之后添加：
ctx.setTools(tools);
```

所有 `executeNode(...)` 递归调用处追加 `tools` 参数（共 4 处：execute 中 1 处、error 边 1 处、condition 分支 1 处、normal 边 1 处）。

新增 import：`import com.hify.mcp.mcp.ToolDef;`

- [ ] **Step 3: WorkflowServiceImpl.run() 透传 tools**

Modify: `hify-workflow/src/main/java/com/hify/workflow/service/impl/WorkflowServiceImpl.java`

```java
// run() 方法中，调用改为：
WorkflowInstanceEntity instance = workflowEngine.execute(
        id, input, req.getSessionId(), triggerType,
        req.getModelConfigId(), req.getTools());
```

- [ ] **Step 4: ChatServiceImpl 解析 Agent 工具并传入 WorkflowRunReq**

Modify: `hify-chat/src/main/java/com/hify/chat/service/impl/ChatServiceImpl.java`

新增依赖注入：
```java
private final McpClientManager mcpClientManager;
```

新增 import：
```java
import com.hify.agent.dto.AgentToolResponse;
import com.hify.mcp.mcp.McpClientManager;
import com.hify.mcp.mcp.ToolDef;
```

新增私有方法：
```java
/** 解析 Agent 绑定的 MCP 工具列表 */
private List<ToolDef> resolveAgentTools(Long agentId) {
    List<AgentToolResponse> agentTools = agentService.getAgentTools(agentId);
    if (agentTools == null || agentTools.isEmpty()) return null;
    List<ToolDef> tools = new ArrayList<>();
    for (AgentToolResponse at : agentTools) {
        if ("mcp".equals(at.getToolType()) && at.getMcpServerId() != null) {
            tools.addAll(mcpClientManager.listTools(at.getMcpServerId()));
        }
    }
    return tools.isEmpty() ? null : tools;
}
```

在 `sendMessage()` 工作流分支中，`runReq.setModelConfigId(...)` 之后添加：
```java
runReq.setTools(resolveAgentTools(ctx.agent.getId()));
```

在 `sendMessageStream()` 工作流分支中，`runReq.setModelConfigId(...)` 之后添加：
```java
runReq.setTools(resolveAgentTools(ctx.agent.getId()));
```

- [ ] **Step 5: 编译验证并提交**

```bash
cd C:/Users/11953/Desktop/hify && ./mvnw compile -pl hify-chat,hify-workflow -am -q
git add hify-chat/ hify-workflow/
git commit -m "feat: Chat 解析 Agent 工具绑定传入工作流，引擎透传到 LLM 节点"
```

---

### Task 7: 前端 LLM 节点工具配置

**Files:**
- Modify: `hify-web/src/api/workflow.ts`
- Modify: `hify-web/src/views/workflow/WorkflowEditor.vue`

- [ ] **Step 1: workflow.ts 类型的 configJson 结构说明**

无需修改类型定义（configJson 已是 string），在 LLM 节点配置中新增字段存储在 configJson 的 JSON 中：
- `toolsEnabled: boolean` — 是否启用工具调用
- `tools: string[]` — 勾选的工具名称列表

- [ ] **Step 2: WorkflowEditor LLM 节点增加工具配置 UI**

在 `<script setup>` 中新增：

```ts
// 工具列表相关
interface AgentToolInfo {
  name: string
  description: string
  serverName: string
}

const agentTools = ref<AgentToolInfo[]>([])

// 加载 Agent 绑定的工具（编辑工作流时通过父组件传入或 API 查询）
async function loadAgentTools(agentId?: number) {
  if (!agentId) return
  try {
    const resp = await agentApi.getAgentTools(agentId)
    agentTools.value = resp.map((t: any) => ({
      name: t.toolName,
      description: t.configJson ? JSON.parse(t.configJson).description || '' : '',
      serverName: t.mcpServerName || ''
    }))
  } catch { /* 非阻塞 */ }
}

// 工具开关
const llmToolsEnabled = computed({
  get: () => {
    const cfg = parseConfig()
    return cfg.toolsEnabled === true
  },
  set: (val: boolean) => {
    const cfg = parseConfig()
    cfg.toolsEnabled = val
    updateConfig(cfg)
  }
})

// 勾选的工具列表
const llmSelectedTools = computed<string[]>({
  get: () => {
    const cfg = parseConfig()
    return cfg.tools || []
  },
  set: (val: string[]) => {
    const cfg = parseConfig()
    cfg.tools = val
    updateConfig(cfg)
  }
})
```

在 LLM 节点配置模板中（Prompt 下方），添加工具配置区域：

```html
<el-form-item label="工具调用">
  <el-switch v-model="llmToolsEnabled" size="small" />
  <span class="type-hint">开启后 LLM 可自主判断是否调用工具</span>
</el-form-item>
<el-form-item v-if="llmToolsEnabled" label="可用工具">
  <el-checkbox-group v-model="llmSelectedTools">
    <div v-if="agentTools.length === 0" class="type-hint">
      当前 Agent 未绑定 MCP 工具
    </div>
    <el-checkbox v-for="t in agentTools" :key="t.name" :label="t.name">
      <span>{{ t.name }}</span>
      <span v-if="t.description" class="type-hint"> — {{ t.description }}</span>
    </el-checkbox>
  </el-checkbox-group>
  <div class="type-hint">继承自 Agent 绑定的 MCP 工具，勾选此节点可用的</div>
</el-form-item>
```

- [ ] **Step 3: 编译验证并提交**

```bash
cd C:/Users/11953/Desktop/hify/hify-web && npx vue-tsc --noEmit 2>&1 | head -30
git add hify-web/
git commit -m "feat: 前端 LLM 节点新增工具调用开关和工具勾选 UI"
```

---

### Task 8: 端到端验证

- [ ] **Step 1: 确保后端编译通过**

```bash
cd C:/Users/11953/Desktop/hify && ./mvnw compile -q
```

- [ ] **Step 2: 确保前端编译通过**

```bash
cd C:/Users/11953/Desktop/hify/hify-web && npx vue-tsc --noEmit
```

- [ ] **Step 3: 手动验证流程**

1. 启动后端和前端
2. 创建一个 Agent，绑定模型 + 工作流 + MCP 工具服务器
3. 在工作流编辑器中配置 LLM 节点，开启工具调用，勾选可用工具
4. 对话触发工作流，验证 LLM 是否自动调用工具
5. 验证多轮工具调用场景
6. 验证工具调用失败时 LLM 能给出合理回复
7. 验证不开启工具调用时 = 纯对话节点（向后兼容）

- [ ] **Step 4: 最终提交**

```bash
git add -A
git commit -m "chore: 端到端验证通过"
```

---

## 验证清单

- [ ] `hify-mcp` 模块编译通过，McpClient 可正常执行 tools/list 和 tools/call
- [ ] `hify-provider` 编译通过，ChatRequest.tools 可为 null（向后兼容）
- [ ] `hify-workflow` 编译通过，LlmNodeExecutor 在无工具时行为不变
- [ ] `hify-chat` 编译通过，Agent 无工具绑定时 tools=null 传入
- [ ] 纯对话节点（不开启工具调用）→ 行为与改造前完全一致
- [ ] 开启工具调用后 → LLM 自主判断并调用工具，最多 3 轮
- [ ] 工具调用失败 → 错误传给 LLM，LLM 给出合理回复
- [ ] 前端 LLM 节点配置面板显示工具开关和勾选列表
- [ ] 前端类型检查通过
