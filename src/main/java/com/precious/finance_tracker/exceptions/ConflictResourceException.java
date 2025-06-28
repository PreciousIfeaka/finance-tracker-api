package com.precious.finance_tracker.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ConflictResourceException extends RuntimeException {
    private final int statusCode = 409;
    public ConflictResourceException(String message) {
        super(message);
    }
}
