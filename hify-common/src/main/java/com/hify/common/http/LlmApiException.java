package com.hify.common.http;

public class LlmApiException extends RuntimeException {

    public static final String TIMEOUT = "TIMEOUT";
    public static final String AUTH_FAILED = "AUTH_FAILED";
    public static final String RATE_LIMITED = "RATE_LIMITED";
    public static final String API_ERROR = "API_ERROR";

    private final String errorType;
    private final int statusCode;

    public LlmApiException(String errorType, String message) {
        super(message);
        this.errorType = errorType;
        this.statusCode = 0;
    }

    public LlmApiException(String errorType, int statusCode, String message) {
        super(message);
        this.errorType = errorType;
        this.statusCode = statusCode;
    }

    public LlmApiException(String errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
        this.statusCode = 0;
    }

    public String getErrorType() {
        return errorType;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
