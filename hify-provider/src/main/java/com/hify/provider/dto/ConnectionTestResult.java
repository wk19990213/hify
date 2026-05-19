package com.hify.provider.dto;

import lombok.Data;

/**
 * 连通性测试结果
 */
@Data
public class ConnectionTestResult {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 延迟毫秒
     */
    private long latencyMs;

    /**
     * 模型数量
     */
    private int modelCount;

    /**
     * 错误信息
     */
    private String errorMessage;
}
