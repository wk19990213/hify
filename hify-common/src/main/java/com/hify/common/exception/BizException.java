package com.hify.common.exception;

import com.hify.common.enums.ErrorCode;
import lombok.Getter;

/**
 * 业务异常
 * 继承 RuntimeException，属于非受检异常
 */
@Getter
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误码枚举（可选）
     */
    private final ErrorCode errorCode;

    /**
     * 使用 ErrorCode 创建异常
     *
     * @param errorCode 错误码枚举
     */
    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.errorCode = errorCode;
    }

    /**
     * 使用 ErrorCode 创建异常，自定义消息覆盖
     *
     * @param errorCode 错误码枚举
     * @param message   自定义消息
     */
    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
        this.errorCode = errorCode;
    }

    /**
     * 使用 ErrorCode 创建异常，支持格式化消息
     *
     * @param errorCode 错误码枚举
     * @param message   消息模板
     * @param args      格式化参数
     */
    public BizException(ErrorCode errorCode, String message, Object... args) {
        super(String.format(message, args));
        this.code = errorCode.getCode();
        this.errorCode = errorCode;
    }

    /**
     * 使用 ErrorCode 创建异常，带原始异常
     *
     * @param errorCode 错误码枚举
     * @param cause     原始异常
     */
    public BizException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.code = errorCode.getCode();
        this.errorCode = errorCode;
    }

    /**
     * 使用 ErrorCode 创建异常，自定义消息，带原始异常
     *
     * @param errorCode 错误码枚举
     * @param message   自定义消息
     * @param cause     原始异常
     */
    public BizException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.code = errorCode.getCode();
        this.errorCode = errorCode;
    }

    /**
     * 快速创建参数错误异常
     */
    public static BizException paramError(String message) {
        return new BizException(ErrorCode.PARAM_ERROR, message);
    }

    /**
     * 快速创建未授权异常
     */
    public static BizException unauthorized(String message) {
        return new BizException(ErrorCode.UNAUTHORIZED, message);
    }

    /**
     * 快速创建数据不存在异常
     */
    public static BizException notFound(String message) {
        return new BizException(ErrorCode.DATA_NOT_EXISTS, message);
    }

    /**
     * 快速创建内部错误异常
     */
    public static BizException internalError(String message) {
        return new BizException(ErrorCode.INTERNAL_ERROR, message);
    }

    /**
     * 快速创建内部错误异常，带原始异常
     */
    public static BizException internalError(String message, Throwable cause) {
        return new BizException(ErrorCode.INTERNAL_ERROR, message, cause);
    }
}
