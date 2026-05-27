package com.hify.workflow.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hify.common.util.TemplateVariableResolver;
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

        query = TemplateVariableResolver.resolve(query, ctx.getVariables());

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

}
