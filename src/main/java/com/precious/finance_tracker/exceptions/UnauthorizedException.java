package com.precious.finance_tracker.exceptions;

import lombok.Getter;

@Getter
public class UnauthorizedException extends RuntimeException {
    private final int statusCode = 401;

    public UnauthorizedException(String message) {
        super(message);
    }
}
