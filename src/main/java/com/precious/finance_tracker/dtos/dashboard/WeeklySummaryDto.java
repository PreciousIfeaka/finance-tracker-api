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
public class WeeklySummaryDto {
    private String weekName;
    private BigDecimal income;
    private BigDecimal expense;
    private BigDecimal budget;
}
