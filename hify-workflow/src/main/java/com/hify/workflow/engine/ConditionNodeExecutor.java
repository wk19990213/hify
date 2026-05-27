package com.hify.workflow.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hify.common.util.TemplateVariableResolver;
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

        expression = TemplateVariableResolver.resolve(expression, ctx.getVariables());
        boolean result = evaluateExpression(expression);
        return NodeExecResult.builder().success(true).output(Map.of("result", result)).build();
    }

    private boolean evaluateExpression(String expr) {
        if (expr.contains("==")) {
            String[] parts = expr.split("==", 2);
            return parts[0].trim().equals(parts[1].trim());
        }
        if (expr.contains("!=")) {
            String[] parts = expr.split("!=", 2);
            return !parts[0].trim().equals(parts[1].trim());
        }
        return "true".equalsIgnoreCase(expr.trim());
    }

}
