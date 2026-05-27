package com.hify.provider.service;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;

/**
 * DUP-03: ToolCallHandler 工具调用处理器。
 * 位于 hify-provider 模块（避免 hify-mcp ↔ hify-provider 循环依赖）。
 */
class ToolCallHandlerTest {

    @Test
    void testToolCallHandlerClassExists() throws ClassNotFoundException {
        Class<?> clazz = Class.forName("com.hify.provider.service.ToolCallHandler");
        assertNotNull(clazz, "ToolCallHandler should exist in hify-provider");
    }

    @Test
    void testHasExecuteToolCallsMethod() throws ClassNotFoundException {
        Class<?> clazz = Class.forName("com.hify.provider.service.ToolCallHandler");
        boolean found = false;
        for (Method m : clazz.getDeclaredMethods()) {
            if ("executeToolCalls".equals(m.getName())) {
                found = true;
                break;
            }
        }
        assertTrue(found, "ToolCallHandler should have executeToolCalls method");
    }
}
