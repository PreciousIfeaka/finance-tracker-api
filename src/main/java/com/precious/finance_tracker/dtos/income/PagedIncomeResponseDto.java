package com.precious.finance_tracker.dtos.income;

import com.precious.finance_tracker.entities.Income;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

@Getter
public class PagedIncomeResponseDto {
    private final List<Income> income;

    private final BigDecimal totalIncome;

    private final int page;

    private final int limit;

    private final Long total;

    public PagedIncomeResponseDto(
            Page<Income> pagedData, BigDecimal totalIncome
    ) {
        this.income = pagedData.getContent();
        this.totalIncome = totalIncome;
        this.page = pagedData.getNumber() + 1;
        this.limit = pagedData.getSize();
        this.total = pagedData.getTotalElements();
    }
}
