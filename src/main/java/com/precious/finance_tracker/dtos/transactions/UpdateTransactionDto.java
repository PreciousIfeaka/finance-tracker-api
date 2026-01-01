package com.precious.finance_tracker.dtos.transactions;

import com.precious.finance_tracker.enums.TransactionDirection;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class UpdateTransactionDto {
    @Positive
    private final BigDecimal amount;

    private final TransactionDirection direction;

    private final String description;
}
