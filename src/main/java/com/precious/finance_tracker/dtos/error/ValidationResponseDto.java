package com.precious.finance_tracker.dtos.error;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ValidationResponseDto {
    private final String field;

    private final String message;
}
