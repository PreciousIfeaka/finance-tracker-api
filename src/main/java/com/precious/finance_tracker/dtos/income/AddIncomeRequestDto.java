package com.precious.finance_tracker.dtos.income;

import com.precious.finance_tracker.enums.IncomeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class AddIncomeRequestDto {
    @NotNull
    private BigDecimal amount;

    @NotBlank
    private String source;

    private IncomeType type;

    @NotBlank
    private LocalDate dateOfReceipt;

    private String note;
}
