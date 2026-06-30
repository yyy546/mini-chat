package com.minichat.common.exception;

public class SpaceException extends BusinessException {
    public SpaceException(ErrorCode errorCode) {
        super(errorCode);
    }

    public SpaceException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
