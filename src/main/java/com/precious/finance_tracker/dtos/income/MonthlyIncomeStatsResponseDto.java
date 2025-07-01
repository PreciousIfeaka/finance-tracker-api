package com.precious.finance_tracker.dtos.income;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthlyIncomeStatsResponseDto(YearMonth month, BigDecimal total) {
}
