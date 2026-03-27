package com.precious.finance_tracker.dtos.transactions;

import com.precious.finance_tracker.enums.TransactionDirection;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTransactionDto {
    @Positive
    private BigDecimal amount;

    @NotNull
    private TransactionDirection direction;

    private LocalDateTime transactionDateTime;

    private String description;
}
