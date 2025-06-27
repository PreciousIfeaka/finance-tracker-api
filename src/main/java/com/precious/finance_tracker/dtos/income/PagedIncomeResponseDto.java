package com.precious.finance_tracker.dtos.income;

import com.precious.finance_tracker.entities.Income;
import org.springframework.data.domain.Page;

import java.util.List;

public class PagedIncomeResponseDto {
    private final List<Income> income;

    private final int page;

    private final int limit;

    private final Long total;

    public PagedIncomeResponseDto(
            Page<Income> pagedData
    ) {
        this.income = pagedData.getContent();
        this.page = pagedData.getNumber() + 1;
        this.limit = pagedData.getSize();
        this.total = pagedData.getTotalElements();
    }
}
