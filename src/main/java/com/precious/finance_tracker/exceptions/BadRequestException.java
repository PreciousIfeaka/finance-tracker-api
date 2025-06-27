package com.precious.finance_tracker.exceptions;

import lombok.Getter;

@Getter
public class BadRequestException extends RuntimeException {
    private final int statusCode = 400;

    public BadRequestException(String message) {
        super(message);
    }
}
