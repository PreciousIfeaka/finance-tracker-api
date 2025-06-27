package com.precious.finance_tracker.dtos.income;

import com.precious.finance_tracker.enums.IncomeType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Builder
@Data
public class UpdateIncomeRequestDto {
    private final BigDecimal amount;

    private final IncomeType type;

    private final String note;

    private final String source;
}
