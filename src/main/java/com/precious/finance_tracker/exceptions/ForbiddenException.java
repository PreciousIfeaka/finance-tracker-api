package com.precious.finance_tracker.exceptions;

import lombok.Getter;

@Getter
public class ForbiddenException extends RuntimeException {
    private final int statusCode = 403;
    public ForbiddenException(String message) {
        super(message);
    }
}
