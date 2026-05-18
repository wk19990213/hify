package com.hify.knowledge.exception;

import com.hify.common.enums.ErrorCode;
import com.hify.common.exception.BizException;

/**
 * Knowledge模块异常
 */
public class KnowledgeException extends BizException {

    public KnowledgeException(ErrorCode errorCode) {
        super(errorCode);
    }

    public KnowledgeException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
