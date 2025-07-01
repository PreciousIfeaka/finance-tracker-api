package com.precious.finance_tracker.dtos.budget;

import com.precious.finance_tracker.enums.ExpenseCategory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateBudgetRequestDto(
        @Positive BigDecimal amount,
        @NotNull ExpenseCategory category,
        @NotNull Boolean isRecurring
) { }
