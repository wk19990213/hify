package com.hify.common.resilience;

import com.hify.common.http.LlmApiException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

/**
 * 按 providerName 管理独立的熔断器实例，每家 LLM 提供商各有独立的熔断器和滑动窗口，
 * 一家熔断不影响其他家。
 */
@Slf4j
@Service
public class CircuitBreakerService {

    private final CircuitBreakerRegistry cbRegistry;

    public CircuitBreakerService(CircuitBreakerRegistry cbRegistry) {
        this.cbRegistry = cbRegistry;
    }

    /**
     * 带熔断+重试保护执行调用。
     *
     * 执行顺序：Retry 循环 → CircuitBreaker 检查 → 实际调用
     * 每次重试都会重新经过熔断器检查，因此重试的失败也会被熔断器统计。
     * 未使用 Resilience4j Retry，而是手动实现重试循环，
     * 原因：超时和限流需要不同的重试间隔（1s vs 2s/4s），Resilience4j 的 Retry
     * 只能按 attempt 次数决定间隔，无法按异常类型区分。
     *
     * 重试规则：
     * - 网络超时(TIMEOUT)：重试2次，每次间隔1s
     * - 限流(RATE_LIMITED)：退避重试2次，间隔2s、4s
     * - 认证失败(AUTH_FAILED)：不重试，直接抛出
     * - 其他错误：不重试，直接抛出
     */
    public <T> T executeWithProtection(String providerName, Supplier<T> supplier) {
        CircuitBreaker cb = getCircuitBreaker(providerName);
        // 外层 lambda：把熔断器包裹在 supplier 外，这样重试循环的每次 get() 调用
        // 都会先经过 CircuitBreaker 的状态检查，熔断开启时抛出 CallNotPermittedException
        return executeWithRetry(providerName, () -> {
            try {
                return cb.executeSupplier(supplier);
            } catch (CallNotPermittedException e) {
                log.warn("熔断器开启: provider={}", providerName);
                throw new LlmApiException(LlmApiException.API_ERROR, "熔断器开启: " + providerName, e);
            }
        });
    }

    /**
     * 按 providerName 获取或创建独立的熔断器实例。
     * Registry 内部以 name 为 key，同一 name 返回同一个 CircuitBreaker 实例，
     * 不同 name 的滑动窗口、失败计数、状态完全隔离。
     */
    public CircuitBreaker getCircuitBreaker(String providerName) {
        return cbRegistry.circuitBreaker(providerName);
    }

    private <T> T executeWithRetry(String providerName, Supplier<T> supplier) {
        int maxRetries = 2;
        LlmApiException lastException = null;

        // supplier.get() 的实际执行路径：重试循环 → 熔断器状态检查 → 真正调用
        // 每次 get() 都是一次完整的"熔断检查 + 真实调用"，失败计数由 CircuitBreaker 内部维护
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return supplier.get();
            } catch (LlmApiException e) {
                lastException = e;
                String type = e.getErrorType();

                if (LlmApiException.AUTH_FAILED.equals(type)) {
                    throw e;
                }

                if (!LlmApiException.TIMEOUT.equals(type) && !LlmApiException.RATE_LIMITED.equals(type)) {
                    throw e;
                }

                if (attempt >= maxRetries) {
                    throw e;
                }

                long waitMs;
                if (LlmApiException.RATE_LIMITED.equals(type)) {
                    waitMs = attempt == 0 ? 2000 : 4000;
                } else {
                    waitMs = 1000;
                }

                log.warn("LLM调用重试: provider={}, errorType={}, attempt={}/{}, waitMs={}",
                        providerName, type, attempt + 1, maxRetries, waitMs);

                try {
                    Thread.sleep(waitMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }

        throw lastException != null ? lastException
                : new LlmApiException(LlmApiException.API_ERROR, "重试异常");
    }
}
