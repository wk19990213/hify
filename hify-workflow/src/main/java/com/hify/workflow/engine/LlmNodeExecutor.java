package com.hify.workflow.engine;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

        prompt = resolveVariables(prompt, ctx.getVariables());

        try {
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
