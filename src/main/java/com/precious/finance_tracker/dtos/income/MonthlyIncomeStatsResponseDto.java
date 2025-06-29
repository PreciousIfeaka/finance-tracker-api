package com.precious.finance_tracker.dtos.income;

import lombok.Data;

import java.math.BigDecimal;

public record MonthlyIncomeStatsResponseDto(String month, BigDecimal total) {
}
