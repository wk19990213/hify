package com.hify.common.enums;

import lombok.Getter;

/**
 * 错误码枚举
 */
@Getter
public enum ErrorCode {

    /**
     * 成功
     */
    SUCCESS(200, "success"),

    /**
     * 参数错误
     */
    PARAM_ERROR(400, "请求参数错误"),

    /**
     * 未授权
     */
    UNAUTHORIZED(401, "未授权"),

    /**
     * 禁止访问
     */
    FORBIDDEN(403, "禁止访问"),

    /**
     * 资源不存在
     */
    NOT_FOUND(404, "资源不存在"),

    /**
     * 请求方法不允许
     */
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),

    /**
     * 请求超时
     */
    REQUEST_TIMEOUT(408, "请求超时"),

    /**
     * 资源冲突
     */
    CONFLICT(409, "资源冲突"),

    /**
     * 请求频率超限
     */
    TOO_MANY_REQUESTS(429, "请求频率超限"),

    /**
     * 系统内部错误
     */
    INTERNAL_ERROR(500, "系统内部错误"),

    /**
     * 服务不可用
     */
    SERVICE_UNAVAILABLE(503, "服务不可用"),

    /**
     * 网关超时
     */
    GATEWAY_TIMEOUT(504, "网关超时"),

    /**
     * 数据库错误
     */
    DATABASE_ERROR(1001, "数据库操作失败"),

    /**
     * 缓存错误
     */
    CACHE_ERROR(1002, "缓存操作失败"),

    /**
     * 远程调用失败
     */
    REMOTE_CALL_ERROR(1003, "远程调用失败"),

    /**
     * 业务规则校验失败
     */
    BIZ_RULE_VIOLATION(2001, "业务规则校验失败"),

    /**
     * 数据已存在
     */
    DATA_EXISTS(2002, "数据已存在"),

    /**
     * 数据不存在
     */
    DATA_NOT_EXISTS(2003, "数据不存在"),

    /**
     * 数据状态异常
     */
    DATA_STATUS_ERROR(2004, "数据状态异常"),

    /**
     * 模型调用失败
     */
    MODEL_CALL_ERROR(3001, "模型调用失败"),

    /**
     * 模型配置错误
     */
    MODEL_CONFIG_ERROR(3002, "模型配置错误"),

    /**
     * 模型限流
     */
    MODEL_RATE_LIMIT(3003, "模型限流"),

    /**
     * MCP工具调用失败
     */
    MCP_CALL_ERROR(4001, "MCP工具调用失败"),

    /**
     * MCP工具配置错误
     */
    MCP_CONFIG_ERROR(4002, "MCP工具配置错误"),

    /**
     * 知识库处理失败
     */
    KNOWLEDGE_PROCESS_ERROR(5001, "知识库处理失败"),

    /**
     * 文档解析失败
     */
    DOCUMENT_PARSE_ERROR(5002, "文档解析失败"),

    /**
     * 工作流执行失败
     */
    WORKFLOW_EXEC_ERROR(6001, "工作流执行失败"),

    /**
     * 工作流配置错误
     */
    WORKFLOW_CONFIG_ERROR(6002, "工作流配置错误");

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误消息
     */
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
