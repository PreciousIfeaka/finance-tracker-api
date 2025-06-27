package com.precious.finance_tracker.dtos.income;

import com.precious.finance_tracker.enums.IncomeType;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AddIncomeRequestDto {
    @NotBlank
    private BigDecimal amount;

    private String source;

    private IncomeType type;

    private String note;
}
