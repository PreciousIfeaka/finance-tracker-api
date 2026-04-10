package com.precious.finance_tracker.dtos.budget;

import com.precious.finance_tracker.enums.ExpenseCategory;

import java.math.BigDecimal;

public record BudgetByCategoryDto(
        ExpenseCategory category,
        BigDecimal total,
        Boolean isExceeded
) {}
