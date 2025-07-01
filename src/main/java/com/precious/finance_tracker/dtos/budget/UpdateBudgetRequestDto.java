package com.precious.finance_tracker.dtos.budget;

import com.precious.finance_tracker.enums.ExpenseCategory;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record UpdateBudgetRequestDto(
        @Positive BigDecimal amount,
        ExpenseCategory category,
        Boolean isRecurring
) { }
