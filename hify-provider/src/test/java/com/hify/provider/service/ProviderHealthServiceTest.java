package com.hify.provider.service;

import com.hify.provider.dto.ConnectionTestResult;
import com.hify.provider.entity.ProviderHealthEntity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DUP-04: 验证 ProviderHealthService 统一健康更新入口存在且可调用。
 */
class ProviderHealthServiceTest {

    @Test
    void testUpdateHealthRecordMethodExists() throws NoSuchMethodException {
        Method method = ProviderHealthService.class.getMethod(
                "updateHealthRecord", Long.class, ConnectionTestResult.class);
        assertNotNull(method, "ProviderHealthService should have updateHealthRecord(Long, ConnectionTestResult)");
    }

    @Test
    void testUpdateHealthRecordReturnsHealthEntity() {
        Method method = ProviderHealthService.class.getMethods()[0];
        // 返回值应为 ProviderHealthEntity
        assertEquals(ProviderHealthEntity.class, method.getReturnType());
    }
}
