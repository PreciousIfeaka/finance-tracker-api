package com.precious.finance_tracker.exceptions;

import lombok.Getter;

@Getter
public class NotFoundException extends RuntimeException {
    private final int statusCode = 404;

    public NotFoundException(String message) {
        super(message);
    }
}
