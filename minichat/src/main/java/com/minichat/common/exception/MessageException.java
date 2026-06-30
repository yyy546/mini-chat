package com.minichat.common.exception;

public class MessageException extends BusinessException {
    public MessageException(ErrorCode errorCode) {
        super(errorCode);
    }

    public MessageException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public MessageException(String message) {
        super(ErrorCode.INTERNAL_ERROR, message);
    }
}
