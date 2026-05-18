package com.hify.chat.exception;

import com.hify.common.enums.ErrorCode;
import com.hify.common.exception.BizException;

/**
 * Chat模块异常
 */
public class ChatException extends BizException {

    public ChatException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ChatException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
