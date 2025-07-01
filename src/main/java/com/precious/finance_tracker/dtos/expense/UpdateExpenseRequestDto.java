package com.precious.finance_tracker.dtos.expense;

import com.precious.finance_tracker.enums.ExpenseCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class UpdateExpenseRequestDto {
    @Positive
    private final BigDecimal amount;

    private final String note;

    private final ExpenseCategory category;

    private Boolean isRecurring;
}
