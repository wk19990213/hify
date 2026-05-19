package com.hify.common.handler;

import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Jackson JSON 类型处理器，用于 MyBatis-Plus 的 JSON 字段映射
 */
public class JacksonTypeHandler extends AbstractJsonTypeHandler<Object> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    protected Object parse(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<Object>() {});
        } catch (Exception e) {
            throw new RuntimeException("JSON parse error: " + json, e);
        }
    }

    @Override
    protected String toJson(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("JSON serialize error", e);
        }
    }
}
