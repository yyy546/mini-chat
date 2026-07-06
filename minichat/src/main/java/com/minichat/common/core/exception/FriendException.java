package com.minichat.common.core.exception;

public class FriendException extends BusinessException {
    public FriendException(ErrorCode errorCode) {
        super(errorCode);
    }

    public FriendException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
