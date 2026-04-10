package com.precious.finance_tracker.dtos.expense;

import com.precious.finance_tracker.enums.ExpenseCategory;

import java.math.BigDecimal;

public record ExpenseByCategoryDto(
        ExpenseCategory category,
        BigDecimal total
) {}
