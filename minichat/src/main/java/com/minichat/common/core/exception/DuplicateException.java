package com.minichat.common.core.exception;

public class DuplicateException extends BusinessException {
    public DuplicateException(ErrorCode errorCode) {
        super(errorCode);
    }

    public DuplicateException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public DuplicateException(String message) {
        super(ErrorCode.CONFLICT, message);
    }
}
