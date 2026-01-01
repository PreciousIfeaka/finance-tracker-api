package com.precious.finance_tracker.dtos.transactions;

import com.precious.finance_tracker.entities.Transactions;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

@Getter
public class PagedTransactionResponseDto {
    private final List<Transactions> transactions;

    private final BigDecimal balance;

    private final int page;

    private final int limit;

    private final Long total;

    public PagedTransactionResponseDto(
            Page<Transactions> pagedData, BigDecimal balance
    ) {
        this.transactions = pagedData.getContent();
        this.balance = balance;
        this.page = pagedData.getNumber() + 1;
        this.limit = pagedData.getSize();
        this.total = pagedData.getTotalElements();
    }
}
