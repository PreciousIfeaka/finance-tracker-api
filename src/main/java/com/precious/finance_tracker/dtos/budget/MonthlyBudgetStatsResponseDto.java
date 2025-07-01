package com.precious.finance_tracker.dtos.budget;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthlyBudgetStatsResponseDto(YearMonth month, BigDecimal total) {}
