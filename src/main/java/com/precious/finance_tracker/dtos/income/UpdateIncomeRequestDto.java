package com.precious.finance_tracker.dtos.income;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Builder
@Data
public class UpdateIncomeRequestDto {
    @Positive
    private final BigDecimal amount;

    private final String note;

    private final String source;

    private final Boolean isRecurring;
}
