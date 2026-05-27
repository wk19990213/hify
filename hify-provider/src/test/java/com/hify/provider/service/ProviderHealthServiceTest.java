package com.hify.provider.service;

import com.hify.provider.dto.ConnectionTestResult;
import com.hify.provider.entity.ProviderHealthEntity;
import com.hify.provider.mapper.ProviderHealthMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * DUP-04: 验证 ProviderHealthService 统一健康更新入口 + TOCTOU 竞态修复。
 */
@ExtendWith(MockitoExtension.class)
class ProviderHealthServiceTest {

    @Mock
    private ProviderHealthMapper providerHealthMapper;

    @InjectMocks
    private ProviderHealthService providerHealthService;

    @Test
    void testUpdateHealthRecordMethodExists() throws NoSuchMethodException {
        Method method = ProviderHealthService.class.getMethod(
                "updateHealthRecord", Long.class, ConnectionTestResult.class);
        assertNotNull(method, "ProviderHealthService should have updateHealthRecord(Long, ConnectionTestResult)");
    }

    @Test
    void testUpdateHealthRecordReturnsHealthEntity() {
        Method method = ProviderHealthService.class.getMethods()[0];
        assertEquals(ProviderHealthEntity.class, method.getReturnType());
    }

    // ===== TOCTOU 竞态测试 =====

    @Test
    void testHandlesDuplicateKeyOnInsert() {
        // TOCTOU: selectById 返回 null，但并发 insert 时发生键冲突
        when(providerHealthMapper.selectById(1L)).thenReturn(null);
        doThrow(new DuplicateKeyException("Duplicate entry"))
                .when(providerHealthMapper).insert(any(ProviderHealthEntity.class));

        // insert 失败后重新查询，返回并发插入的记录
        ProviderHealthEntity existing = new ProviderHealthEntity();
        existing.setProviderId(1L);
        existing.setConsecutiveFailures(0);
        when(providerHealthMapper.selectById(1L)).thenReturn(null, existing);

        ConnectionTestResult result = new ConnectionTestResult();
        result.setSuccess(true);
        result.setLatencyMs(100);
        ProviderHealthEntity returned = providerHealthService.updateHealthRecord(1L, result);

        // 应回退到 updateById 而非继续抛异常
        verify(providerHealthMapper).updateById(any(ProviderHealthEntity.class));
        assertEquals(100, returned.getAvgLatencyMs());
    }
}
