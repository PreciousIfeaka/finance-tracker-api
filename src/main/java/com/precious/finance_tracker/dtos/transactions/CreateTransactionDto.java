package com.precious.finance_tracker.dtos.transactions;

import com.precious.finance_tracker.enums.TransactionDirection;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CreateTransactionDto {
    @Positive
    private final BigDecimal amount;

    @NotNull
    private final TransactionDirection direction;

    private String description;
}
