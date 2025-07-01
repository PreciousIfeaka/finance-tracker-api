package com.precious.finance_tracker.dtos.expense;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthlyExpenseStatsResponseDto(YearMonth month, BigDecimal total) {}
