package com.hify.mcp.exception;

import com.hify.common.enums.ErrorCode;
import com.hify.common.exception.BizException;

/**
 * Mcp模块异常
 */
public class McpException extends BizException {

    public McpException(ErrorCode errorCode) {
        super(errorCode);
    }

    public McpException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
