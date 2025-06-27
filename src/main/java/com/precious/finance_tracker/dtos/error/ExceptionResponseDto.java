package com.precious.finance_tracker.dtos.error;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ExceptionResponseDto {
    private final String status;
    private final String message;
    private final int statusCode;
}
