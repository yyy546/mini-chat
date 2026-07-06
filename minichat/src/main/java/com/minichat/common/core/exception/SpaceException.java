package com.minichat.common.core.exception;

public class SpaceException extends BusinessException {
    public SpaceException(ErrorCode errorCode) {
        super(errorCode);
    }

    public SpaceException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
