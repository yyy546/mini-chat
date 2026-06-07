package com.minichat.common.exception;

public class MessageException extends BusinessException {
    public MessageException(String message) {
        super(500, message);
    }
}
