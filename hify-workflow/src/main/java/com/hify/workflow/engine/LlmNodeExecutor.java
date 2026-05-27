package com.hify.workflow.engine;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hify.common.util.JsonUtils;
import com.hify.common.util.TemplateVariableResolver;
import com.hify.provider.util.AuthConfigHelper;
import com.hify.mcp.mcp.McpClientManager;
import com.hify.mcp.mcp.ToolDef;
import com.hify.mcp.mcp.ToolResult;
import com.hify.provider.adapter.ChatRequest;
import com.hify.provider.adapter.ProviderAdapter;
import com.hify.provider.adapter.ProviderAdapterFactory;
import com.hify.provider.entity.ModelConfigEntity;
import com.hify.provider.entity.ProviderEntity;

import com.hify.provider.mapper.ModelConfigMapper;
import com.hify.provider.service.ProviderDiscoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class LlmNodeExecutor implements NodeExecutor {

    private final ModelConfigMapper modelConfigMapper;
    private final ProviderDiscoveryService providerDiscoveryService;
    private final ProviderAdapterFactory adapterFactory;
    private final ObjectMapper objectMapper;
    private final McpClientManager mcpClientManager;

    @Override
    public String getType() {
        return "llm";
    }

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

        prompt = TemplateVariableResolver.resolve(prompt, ctx.getVariables());

        try {
            ModelConfigEntity modelConfig = modelConfigMapper.selectById(modelConfigId);
            if (modelConfig == null || modelConfig.getDeleted() == 1) {
                return NodeExecResult.builder().success(false)
                        .errorMsg("模型配置不存在").build();
            }
            ProviderEntity provider = findProvider(modelConfig.getModelId());
            if (provider == null) {
                return NodeExecResult.builder().success(false)
                        .errorMsg("没有可用的模型提供商").build();
            }

            ProviderAdapter adapter = adapterFactory.getAdapter(provider.getType());
            Map<String, Object> authConfig = decryptAuth(provider);

            // 构建初始 messages
            List<Map<String, Object>> messages = new ArrayList<>();
            messages.add(Map.of("role", "user", "content", prompt));

            // 按节点配置过滤工具列表
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
                    break;
                }

                // 追加 assistant 消息（含 tool_calls）
                List<Map<String, Object>> tcMaps = new ArrayList<>();
                for (ProviderAdapter.ToolCall tc : toolCalls) {
                    Map<String, Object> func = new LinkedHashMap<>();
                    func.put("name", tc.getName());
                    func.put("arguments", JsonUtils.toJson(tc.getArguments()));
                    tcMaps.add(Map.of("id", tc.getId() != null ? tc.getId() : "",
                            "type", "function", "function", func));
                }
                Map<String, Object> asstMsg = new LinkedHashMap<>();
                asstMsg.put("role", "assistant");
                asstMsg.put("content", lastContent != null ? lastContent : "");
                asstMsg.put("tool_calls", tcMaps);
                messages.add(asstMsg);

                for (ProviderAdapter.ToolCall tc : toolCalls) {
                    ToolResult tr = executeToolCall(tc, tools);
                    Map<String, Object> toolMsg = new LinkedHashMap<>();
                    toolMsg.put("role", "tool");
                    toolMsg.put("tool_call_id", tc.getId() != null ? tc.getId() : "");
                    toolMsg.put("content",
                            tr.isSuccess() ? tr.getContent()
                                    : "Error: " + tr.getError());
                    messages.add(toolMsg);
                }

                if (lastContent != null && !lastContent.isBlank()) {
                    break;
                }
            }

            Map<String, Object> output = new LinkedHashMap<>();
            output.put("content", lastContent != null ? lastContent : "");
            tryMergeStructuredOutput(lastContent, output);

            return NodeExecResult.builder().success(true).output(output).build();
        } catch (Exception e) {
            log.error("LLM node execution failed: nodeId={}", ctx.getNode().getId(), e);
            return NodeExecResult.builder().success(false)
                    .errorMsg("LLM 调用失败: " + e.getMessage()).build();
        }
    }

    /** 按节点配置检查是否启用工具 */
    private List<ToolDef> resolveNodeTools(Map<String, Object> config,
                                            List<ToolDef> allTools) {
        if (allTools == null || allTools.isEmpty()) return null;
        Boolean enabled = (Boolean) config.get("toolsEnabled");
        if (!Boolean.TRUE.equals(enabled)) return null;
        return allTools;
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

    private ProviderEntity findProvider(String modelId) {
        return providerDiscoveryService.findAvailableProviderByModelId(modelId);
    }

    private Map<String, Object> decryptAuth(ProviderEntity provider) {
        return AuthConfigHelper.decryptAuthConfig(provider.getAuthConfig());
    }

    /** 尝试从 LLM 响应中提取 JSON 结构并合并到 output Map。 */
    void tryMergeStructuredOutput(String content, Map<String, Object> output) {
        String jsonStr = extractJson(content);
        if (jsonStr != null) {
            try {
                Map<String, Object> parsed = objectMapper.readValue(jsonStr,
                        new TypeReference<Map<String, Object>>() {});
                output.putAll(parsed);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 从 LLM 响应中提取 JSON，支持 markdown 代码块包裹的格式。
     */
    private String extractJson(String content) {
        if (content == null || content.isBlank()) return null;
        String trimmed = content.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) return trimmed;
        int start = trimmed.indexOf("{");
        int end = trimmed.lastIndexOf("}");
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return null;
    }

}
