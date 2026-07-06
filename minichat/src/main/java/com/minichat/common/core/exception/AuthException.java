package com.minichat.common.core.exception;

public class AuthException extends BusinessException {
    public AuthException(ErrorCode errorCode) {
        super(errorCode);
    }

    public AuthException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public AuthException(String message) {
        super(ErrorCode.UNAUTHORIZED, message);
    }
}
