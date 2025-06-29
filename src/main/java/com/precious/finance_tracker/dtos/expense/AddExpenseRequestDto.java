package com.precious.finance_tracker.dtos.expense;

import com.precious.finance_tracker.enums.ExpenseCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class AddExpenseRequestDto {
    @PositiveOrZero
    private final BigDecimal amount;

    private String note;

    private LocalDate dueDate;

    private final ExpenseCategory category;
}
