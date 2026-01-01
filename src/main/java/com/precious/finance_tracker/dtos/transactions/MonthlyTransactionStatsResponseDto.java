package com.precious.finance_tracker.dtos.transactions;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthlyTransactionStatsResponseDto(YearMonth month, BigDecimal total) {
}
