package com.minichat.common.exception;

public class SessionException extends BusinessException {
    public SessionException(ErrorCode errorCode) {
        super(errorCode);
    }

    public SessionException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
