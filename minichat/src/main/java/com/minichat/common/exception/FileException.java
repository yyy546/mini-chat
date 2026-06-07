package com.minichat.common.exception;

public class FileException extends BusinessException {
    public FileException(String message) {
        super(500, message);
    }
}
