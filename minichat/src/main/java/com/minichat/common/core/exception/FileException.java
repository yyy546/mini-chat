package com.minichat.common.core.exception;

public class FileException extends BusinessException {
    public FileException(ErrorCode errorCode) {
        super(errorCode);
    }

    public FileException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public FileException(String message) {
        super(ErrorCode.INTERNAL_ERROR, message);
    }
}
