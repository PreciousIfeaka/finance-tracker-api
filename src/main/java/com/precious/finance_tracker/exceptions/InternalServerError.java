package com.precious.finance_tracker.exceptions;

import lombok.Getter;

@Getter
public class InternalServerError extends RuntimeException {
    private final int statusCode = 500;
    public InternalServerError(String message) {
        super(message);
    }
}
