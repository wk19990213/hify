package com.hify.workflow.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CPX-03: 验证 LlmNodeExecutor.tryMergeStructuredOutput 方法存在。
 */
class LlmNodeExecutorTest {

    @Test
    void testTryMergeStructuredOutputMethodExists() throws NoSuchMethodException {
        Method method = LlmNodeExecutor.class.getDeclaredMethod(
                "tryMergeStructuredOutput", String.class, Map.class);
        assertNotNull(method, "should have tryMergeStructuredOutput(String, Map)");
    }

    @Test
    void testMethodIsAccessible() throws NoSuchMethodException {
        Method method = LlmNodeExecutor.class.getDeclaredMethod(
                "tryMergeStructuredOutput", String.class, Map.class);
        method.setAccessible(true);
        assertNotNull(method);
    }
}
