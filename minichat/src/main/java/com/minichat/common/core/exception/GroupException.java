package com.minichat.common.core.exception;

public class GroupException extends BusinessException {
    public GroupException(ErrorCode errorCode) {
        super(errorCode);
    }

    public GroupException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
