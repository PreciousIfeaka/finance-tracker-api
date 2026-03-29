package com.precious.finance_tracker.dtos.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CurrentMonthSummaryDto {
    private BigDecimal totalIncome;
    private double incomePercentageChange;

    private BigDecimal totalExpense;
    private double expensePercentageChange;

    private BigDecimal totalBudget;
    private double budgetPercentageChange;
}
