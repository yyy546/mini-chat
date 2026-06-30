package com.minichat.common.exception;

public class SystemException extends BusinessException {
    public SystemException(ErrorCode errorCode) {
        super(errorCode);
    }

    public SystemException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public SystemException(String message) {
        super(ErrorCode.INTERNAL_ERROR, message);
    }
}
