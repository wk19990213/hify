package com.hify.workflow.exception;

import com.hify.common.enums.ErrorCode;
import com.hify.common.exception.BizException;

/**
 * Workflow模块异常
 */
public class WorkflowException extends BizException {

    public WorkflowException(ErrorCode errorCode) {
        super(errorCode);
    }

    public WorkflowException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
