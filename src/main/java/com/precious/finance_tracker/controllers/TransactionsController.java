package com.precious.finance_tracker.controllers;


import com.precious.finance_tracker.dtos.BaseResponseDto;
import com.precious.finance_tracker.dtos.transactions.MonthlyTransactionStatsResponseDto;
import com.precious.finance_tracker.dtos.transactions.PagedTransactionResponseDto;
import com.precious.finance_tracker.dtos.transactions.UpdateTransactionDto;
import com.precious.finance_tracker.entities.Transactions;
import com.precious.finance_tracker.enums.TransactionDirection;
import com.precious.finance_tracker.services.TransactionsService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@Data
@Tag(name = "Transactions")
@SecurityRequirement(name = "bearerAuth")
public class TransactionsController {
    private final TransactionsService transactionsService;

    @PutMapping("/{id}")
    public ResponseEntity<BaseResponseDto<Transactions>> updateTransactions(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateTransactionDto dto
    ) {
        return ResponseEntity
                .ok(this.transactionsService.updateTransaction(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponseDto<Transactions>> getTransaction(
            @PathVariable("id") UUID id
    ) {
        return ResponseEntity.ok(this.transactionsService.getTransactionById(id));
    }

    @GetMapping()
    public ResponseEntity<BaseResponseDto<PagedTransactionResponseDto>> getAllTransactionsByMonth(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer limit,
            @RequestParam(required = false, value = "date") @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @RequestParam(required = false, value = "direction") TransactionDirection direction
            ) {

        return ResponseEntity.ok(
                this.transactionsService.getFilteredTransactions(
                        page, limit, month, direction
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponseDto<Object>> deleteTransactions(
            @PathVariable("id") UUID id
    ) {
        return ResponseEntity.ok(this.transactionsService.deleteTransactionById(id));
    }

    @GetMapping("/monthly-totals")
    public ResponseEntity<BaseResponseDto<List<MonthlyTransactionStatsResponseDto>>> getMonthlyTotals(
            @RequestParam(required = false, value = "direction") TransactionDirection direction
    ) {
        return ResponseEntity.ok(
                this.transactionsService.getMonthlyTransactionsStats(direction)
        );
    }
}
