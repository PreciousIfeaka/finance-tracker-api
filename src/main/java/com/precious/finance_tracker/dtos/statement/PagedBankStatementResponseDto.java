package com.precious.finance_tracker.dtos.statement;

import com.precious.finance_tracker.entities.BankStatement;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

@Getter
public class PagedBankStatementResponseDto {
    private final List<BankStatement> statements;

    private final int page;

    private final int limit;

    private final Long total;

    public PagedBankStatementResponseDto(Page<BankStatement> pagedData) {
        this.statements = pagedData.getContent();
        this.page = pagedData.getNumber() + 1;
        this.limit = pagedData.getSize();
        this.total = pagedData.getTotalElements();
    }
}
