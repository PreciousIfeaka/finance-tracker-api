package com.precious.finance_tracker.controllers;

import com.precious.finance_tracker.dtos.BaseResponseDto;
import com.precious.finance_tracker.dtos.budget.*;
import com.precious.finance_tracker.entities.Budget;
import com.precious.finance_tracker.services.interfaces.IBudgetService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/v1/budgets")
@Data
@Tag(name = "Budgets")
@SecurityRequirement(name = "bearerAuth")
public class BudgetController {
    private final IBudgetService budgetService;

    @PostMapping()
    public ResponseEntity<BaseResponseDto<Budget>> addBudget(
            @RequestBody CreateBudgetRequestDto dto
    ) {
        return ResponseEntity.
                status(HttpStatus.CREATED)
                .body(this.budgetService.createBudget(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BaseResponseDto<Budget>> updateBudget(
            @PathVariable("id") UUID id,
            @RequestBody UpdateBudgetRequestDto dto
            ) {
        return ResponseEntity
                .ok(this.budgetService.updateBudget(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponseDto<Budget>> getBudget(
            @PathVariable("id") UUID id
    ) {
        return ResponseEntity.ok(this.budgetService.getBudget(id));
    }

    @GetMapping("/month")
    public ResponseEntity<BaseResponseDto<PagedBudgetResponseDto>> getAllBudgetsByMonth(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer limit,
            @RequestParam(required = false, value = "date") @DateTimeFormat(pattern = "yyyy-MM") YearMonth month
            ) {
        YearMonth defaultMonth = YearMonth.now();

        return ResponseEntity.ok(
                this.budgetService.getAllBudgetsByMonth(
                        page, limit, month != null ? month : defaultMonth
                )
        );
    }

    @GetMapping()
    public ResponseEntity<BaseResponseDto<PagedBudgetResponseDto>> getAllBudgets(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer limit
    ) {

        return ResponseEntity.ok(
                this.budgetService.getAllBudgets(page, limit)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponseDto<Object>> deleteBudget(
            @PathVariable("id") UUID id
    ) {
        return ResponseEntity.ok(this.budgetService.deleteBudgetById(id));
    }

    @GetMapping("/monthly-totals")
    public ResponseEntity<BaseResponseDto<List<MonthlyBudgetStatsResponseDto>>> getMonthlyTotals() {
        return ResponseEntity.ok(
                this.budgetService.getMonthlyBudgetStats()
        );
    }
}
