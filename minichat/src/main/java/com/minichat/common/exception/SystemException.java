package com.minichat.common.exception;

public class SystemException extends BusinessException {
    public SystemException(String message) {
        super(500, message);
    }
}
