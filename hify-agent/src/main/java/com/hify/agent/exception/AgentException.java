package com.hify.agent.exception;

import com.hify.common.enums.ErrorCode;
import com.hify.common.exception.BizException;

/**
 * Agent模块异常
 */
public class AgentException extends BizException {

    public AgentException(ErrorCode errorCode) {
        super(errorCode);
    }

    public AgentException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
