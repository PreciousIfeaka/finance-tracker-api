package com.precious.finance_tracker.dtos.income;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AddIncomeRequestDto {
    @Positive
    private BigDecimal amount;

    @NotBlank
    private String source;

    @NotNull
    private Boolean isRecurring;

    private String note;
}
