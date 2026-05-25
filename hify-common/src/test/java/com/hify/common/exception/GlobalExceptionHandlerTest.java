package com.hify.common.exception;

import com.hify.common.enums.ErrorCode;
import com.hify.common.result.Result;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * P1-4: 异常信息泄露测试
 * 所有非业务异常的 handler 不得返回原始异常消息，仅记录日志
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleIllegalArgumentException_ShouldNotLeakRawMessage() {
        // 即使异常包含敏感信息，也不应返回给客户端
        IllegalArgumentException ex = new IllegalArgumentException(
                "Connection refused to jdbc:mysql://192.168.1.1:3306 with user=admin, password=secret123");

        Result<Void> result = handler.handleIllegalArgumentException(ex);

        assertNotNull(result);
        // 不能返回原始错误信息（可能包含凭据、IP 等敏感信息）
        assertFalse(result.getMessage().contains("secret123"),
                "响应不应包含敏感信息");
        assertFalse(result.getMessage().contains("password"),
                "响应不应包含敏感信息");
        assertFalse(result.getMessage().contains("192.168.1.1"),
                "响应不应包含内网 IP");
    }

    @Test
    void handleIllegalStateException_ShouldNotLeakRawMessage() {
        IllegalStateException ex = new IllegalStateException(
                "Failed to connect to redis://172.16.0.5:6379 with auth token abc123xyz");

        Result<Void> result = handler.handleIllegalStateException(ex);

        assertNotNull(result);
        assertFalse(result.getMessage().contains("abc123xyz"),
                "响应不应包含敏感 token");
        assertFalse(result.getMessage().contains("172.16.0.5"),
                "响应不应包含内网 IP");
    }

    @Test
    void handleException_ShouldReturnGenericMessage() {
        // 兜底异常处理 - 必须返回通用消息
        Exception ex = new RuntimeException("Database connection pool exhausted for jdbc:mysql://db.internal:3306");

        Result<Void> result = handler.handleException(ex);

        assertNotNull(result);
        assertNotNull(result.getMessage());
        // 通用异常响应不应包含原始消息
        assertFalse(result.getMessage().contains("jdbc:mysql"),
                "异常响应不应包含数据库连接信息");
        assertFalse(result.getMessage().contains("db.internal"),
                "响应不应包含内网主机名");
    }

    @Test
    void handleBizException_ShouldReturnBizMessage() {
        // 业务异常是有意抛出的，消息经过设计，应正常返回
        BizException ex = new BizException(ErrorCode.PARAM_ERROR, "知识库名称不能为空");

        Result<Void> result = handler.handleBizException(ex);

        assertNotNull(result);
        assertEquals(ErrorCode.PARAM_ERROR.getCode().intValue(), result.getCode().intValue());
        assertEquals("知识库名称不能为空", result.getMessage());
    }

    @Test
    void handleIllegalArgumentException_NullMessage_ShouldNotThrow() {
        IllegalArgumentException ex = new IllegalArgumentException();

        Result<Void> result = handler.handleIllegalArgumentException(ex);

        assertNotNull(result);
        assertNotNull(result.getMessage());
    }
}
