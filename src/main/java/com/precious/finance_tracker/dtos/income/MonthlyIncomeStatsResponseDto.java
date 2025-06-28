package com.precious.finance_tracker.dtos.income;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MonthlyIncomeStatsResponseDto {
    private final String month;

    private final BigDecimal total;
}
