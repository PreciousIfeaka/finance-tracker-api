package com.precious.finance_tracker.dtos.budget;

import com.precious.finance_tracker.entities.Budget;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

@Getter
public class PagedBudgetResponseDto {
    private final List<Budget> budgets;

    private final int page;

    private final int limit;

    private final Long total;

    private final BigDecimal totalBudget;

    public PagedBudgetResponseDto(
            Page<Budget> pagedData, BigDecimal totalBudget
    ) {
        this.budgets = pagedData.getContent();
        this.totalBudget = totalBudget;
        this.page = pagedData.getNumber() + 1;
        this.limit = pagedData.getSize();
        this.total = pagedData.getTotalElements();
    }
}
