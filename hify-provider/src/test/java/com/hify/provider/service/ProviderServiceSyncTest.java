package com.hify.provider.service;

import com.hify.provider.entity.ProviderEntity;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DUP-05: 验证 ProviderService 接口暴露 syncModels 方法，
 * ProviderHealthScheduler 可通过该接口委托同步逻辑。
 */
class ProviderServiceSyncTest {

    @Test
    void testProviderServiceHasSyncModelsMethod() throws NoSuchMethodException {
        Method method = ProviderService.class.getMethod("syncModels", ProviderEntity.class, Map.class);
        assertNotNull(method, "ProviderService should expose syncModels(ProviderEntity, Map)");
    }

    @Test
    void testSyncModelsSignature() throws NoSuchMethodException {
        Method method = ProviderService.class.getMethod("syncModels", ProviderEntity.class, Map.class);
        assertEquals("syncModels", method.getName());
        assertEquals(2, method.getParameterCount());
    }
}
