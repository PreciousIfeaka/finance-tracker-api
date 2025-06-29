package com.precious.finance_tracker.dtos.expense;

import com.precious.finance_tracker.entities.Expense;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
public class PagedExpenseResponseDto {
    private final List<Expense> expenses;

    private final int page;

    private final int limit;

    private final Long total;

    public PagedExpenseResponseDto(
            Page<Expense> pagedData
    ) {
        this.expenses = pagedData.getContent();
        this.page = pagedData.getNumber() + 1;
        this.limit = pagedData.getSize();
        this.total = pagedData.getTotalElements();
    }
}
