package com.minichat.common.core.exception;

public class ValidationException extends BusinessException {
    public ValidationException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ValidationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public ValidationException(String message) {
        super(ErrorCode.BAD_REQUEST, message);
    }
}
