package com.precious.finance_tracker.controllers;

import com.precious.finance_tracker.dtos.BaseResponseDto;
import com.precious.finance_tracker.dtos.expense.AddExpenseRequestDto;
import com.precious.finance_tracker.dtos.expense.MonthlyExpenseStatsResponseDto;
import com.precious.finance_tracker.dtos.expense.PagedExpenseResponseDto;
import com.precious.finance_tracker.dtos.expense.UpdateExpenseRequestDto;
import com.precious.finance_tracker.entities.Expense;
import com.precious.finance_tracker.services.ExpenseService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/expenses")
@Data
@Tag(name = "Expenses")
@SecurityRequirement(name = "bearerAuth")
public class ExpenseController {
    private final ExpenseService expenseService;

    @PostMapping()
    public ResponseEntity<BaseResponseDto<Expense>> addExpense(
            @Valid @RequestBody AddExpenseRequestDto dto
    ) {
        return ResponseEntity.
                status(HttpStatus.CREATED)
                .body(this.expenseService.addExpenseData(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BaseResponseDto<Expense>> updateExpense(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateExpenseRequestDto dto
    ) {
        return ResponseEntity
                .ok(this.expenseService.updateExpense(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponseDto<Expense>> getExpense(
            @PathVariable("id") UUID id
    ) {
        return ResponseEntity.ok(this.expenseService.getExpenseById(id));
    }

    @GetMapping("/month")
    public ResponseEntity<BaseResponseDto<PagedExpenseResponseDto>> getAllExpensesByMonth(
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer limit,
            @RequestParam(required = false, value = "date") @DateTimeFormat(pattern = "yyyy-MM")YearMonth month
            ) {
        YearMonth defaultMonth = YearMonth.now();

        return ResponseEntity.ok(
                this.expenseService.getAllExpensesByMonth(
                        page, limit, month != null ? month : defaultMonth
                )
        );
    }

    @GetMapping()
    public ResponseEntity<BaseResponseDto<PagedExpenseResponseDto>> getAllExpenses(
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer limit
    ) {

        return ResponseEntity.ok(
                this.expenseService.getAllExpenses(page, limit)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponseDto<Object>> deleteExpense(
            @PathVariable("id") UUID id
    ) {
        return ResponseEntity.ok(this.expenseService.deleteExpenseById(id));
    }

    @GetMapping("/monthly-totals")
    public ResponseEntity<BaseResponseDto<List<MonthlyExpenseStatsResponseDto>>> getMonthlyTotals() {
        return ResponseEntity.ok(
                this.expenseService.getMonthlyExpenseStats()
        );
    }
}
