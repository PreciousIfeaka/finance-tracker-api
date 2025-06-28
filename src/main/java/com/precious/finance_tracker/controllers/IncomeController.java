package com.precious.finance_tracker.controllers;

import com.precious.finance_tracker.dtos.BaseResponseDto;
import com.precious.finance_tracker.dtos.income.AddIncomeRequestDto;
import com.precious.finance_tracker.dtos.income.MonthlyIncomeStatsResponseDto;
import com.precious.finance_tracker.dtos.income.PagedIncomeResponseDto;
import com.precious.finance_tracker.dtos.income.UpdateIncomeRequestDto;
import com.precious.finance_tracker.entities.Income;
import com.precious.finance_tracker.services.IncomeService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incomes")
@Data
@Tag(name = "Incomes")
@SecurityRequirement(name = "bearerAuth")
public class IncomeController {
    private final IncomeService incomeService;

    @PostMapping()
    public ResponseEntity<BaseResponseDto<Income>> addIncome(
            @RequestBody AddIncomeRequestDto dto
    ) {
        return ResponseEntity.
                status(HttpStatus.CREATED)
                .body(this.incomeService.addIncome(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BaseResponseDto<Income>> updateIncome(
            @PathVariable("id") UUID id,
            @RequestBody UpdateIncomeRequestDto dto
    ) {
        return ResponseEntity
                .ok(this.incomeService.updateIncome(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponseDto<Income>> getIncome(
            @PathVariable("id") UUID id
    ) {
        return ResponseEntity.ok(this.incomeService.getIncomeById(id));
    }

    @GetMapping("/month")
    public ResponseEntity<BaseResponseDto<PagedIncomeResponseDto>> getAllIncomesByMonth(
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer limit,
            @RequestParam(required = false, value = "date") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date
    ) {
        LocalDate defaultDate = LocalDate.now();

        return ResponseEntity.ok(
                this.incomeService.getAllIncomesByMonth(
                        page, limit, date != null ? date : defaultDate
                )
        );
    }

    @GetMapping()
    public ResponseEntity<BaseResponseDto<PagedIncomeResponseDto>> getAllIncomes(
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer limit
    ) {

        return ResponseEntity.ok(
                this.incomeService.getAllIncomes(page, limit)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponseDto<Object>> deleteIncome(
            @PathVariable("id") UUID id
    ) {
        return ResponseEntity.ok(this.incomeService.deleteIncomeById(id));
    }

    @GetMapping("/monthly-totals")
    public ResponseEntity<BaseResponseDto<List<MonthlyIncomeStatsResponseDto>>> getMonthlyTotals() {
        return ResponseEntity.ok(
                this.incomeService.getMonthlyIncomeStats()
        );
    }
}
