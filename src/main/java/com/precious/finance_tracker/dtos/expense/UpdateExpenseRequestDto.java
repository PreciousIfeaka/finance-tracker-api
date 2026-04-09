package com.precious.finance_tracker.dtos.expense;

import com.precious.finance_tracker.enums.ExpenseCategory;
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
public class UpdateExpenseRequestDto {
    @Positive
    private BigDecimal amount;

    private String note;

    private ExpenseCategory category;

    private Boolean isRecurring;

    private LocalDateTime transactionDateTime;
}
