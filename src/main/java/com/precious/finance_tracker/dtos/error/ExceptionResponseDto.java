package com.precious.finance_tracker.dtos.error;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExceptionResponseDto {
    private String status;
    private String message;
    private int statusCode;
}
