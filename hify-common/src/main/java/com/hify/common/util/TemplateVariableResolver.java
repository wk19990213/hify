package com.hify.common.util;

import java.util.Map;

/**
 * 模板变量解析器 — 替换 {{key}} 和 {{key.field}} 占位符。
 * 用于工作流节点执行器中的 prompt/url/expression 模板变量替换。
 */
public class TemplateVariableResolver {

    /**
     * 替换模板中的 {{key}} 和 {{key.field}} 占位符。
     * @param template  包含占位符的模板字符串
     * @param variables 变量 Map
     * @return 替换后的字符串
     */
    public static String resolve(String template, Map<String, Object> variables) {
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
