package com.precious.finance_tracker.dtos.expense;

import java.math.BigDecimal;

public record MonthlyExpenseStatsResponseDto(String month, BigDecimal total) {}
