package com.hify.common.util;

import com.hify.common.exception.BizException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 实体存在性验证测试 - P3-3
 */
class EntityValidatorTest {

    @Test
    void testRequireNonNull_ReturnsValue() {
        String value = "valid-entity";
        String result = EntityValidator.requireNonNull(value, "资源不存在");
        assertEquals("valid-entity", result);
    }

    @Test
    void testRequireNonNull_Null_ThrowsNotFound() {
        BizException ex = assertThrows(BizException.class, () -> {
            EntityValidator.requireNonNull(null, "知识库不存在");
        });
        assertTrue(ex.getMessage().contains("知识库不存在"));
    }

    @Test
    void testRequireNonNull_CustomMessage() {
        BizException ex = assertThrows(BizException.class, () -> {
            EntityValidator.requireNonNull(null, "Agent 配置不存在");
        });
        assertEquals("Agent 配置不存在", ex.getMessage());
    }
}
