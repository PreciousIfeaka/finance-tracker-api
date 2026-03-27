package com.precious.finance_tracker.dtos.income;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateIncomeRequestDto {
    @Positive
    private BigDecimal amount;

    private String note;

    private String source;

    private Boolean isRecurring;

    private LocalDateTime transactionDateTime;
}
