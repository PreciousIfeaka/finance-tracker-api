package com.precious.finance_tracker.dtos.expense;

import com.precious.finance_tracker.entities.Expense;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

@Getter
public class PagedExpenseResponseDto {
    private final List<Expense> expenses;

    private final BigDecimal totalExpenses;

    private final int page;

    private final int limit;

    private final Long total;

    public PagedExpenseResponseDto(
            Page<Expense> pagedData, BigDecimal totalExpenses
    ) {
        this.expenses = pagedData.getContent();
        this.totalExpenses = totalExpenses;
        this.page = pagedData.getNumber() + 1;
        this.limit = pagedData.getSize();
        this.total = pagedData.getTotalElements();
    }
}
