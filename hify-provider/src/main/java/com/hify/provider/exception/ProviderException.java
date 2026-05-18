package com.hify.provider.exception;

import com.hify.common.enums.ErrorCode;
import com.hify.common.exception.BizException;

/**
 * Provider模块异常
 */
public class ProviderException extends BizException {

    public ProviderException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ProviderException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
