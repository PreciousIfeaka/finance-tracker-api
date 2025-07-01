package com.precious.finance_tracker.dtos.expense;

import com.precious.finance_tracker.enums.ExpenseCategory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AddExpenseRequestDto {
    @Positive
    private final BigDecimal amount;

    private final String note;

    @NotNull
    private final ExpenseCategory category;

    @NotNull
    private final Boolean isRecurring;
}
