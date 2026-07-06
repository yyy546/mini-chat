package com.minichat.common.core.exception;

public class ChatException extends BusinessException {
    public ChatException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ChatException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
