package com.crafts.profileservice.exception;

import lombok.Getter;

@Getter
public class UserProfileBusinessException extends RuntimeException {

    private final String errorMessage;

    public UserProfileBusinessException(String errorMessage) {
        super(errorMessage);
        this.errorMessage = errorMessage;
    }

    public UserProfileBusinessException(String errorMessage, Throwable cause) {
        super(errorMessage, cause);
        this.errorMessage = errorMessage;
    }

    @Override
    public String getMessage() {
        return errorMessage;
    }
}
