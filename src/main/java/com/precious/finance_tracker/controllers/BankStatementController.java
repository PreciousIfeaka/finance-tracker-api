package com.precious.finance_tracker.controllers;

import com.precious.finance_tracker.dtos.BaseResponseDto;
import com.precious.finance_tracker.dtos.statement.CreateStatementRequestDto;
import com.precious.finance_tracker.dtos.statement.PagedBankStatementResponseDto;
import com.precious.finance_tracker.dtos.statement.UpdateStatementRequestDto;
import com.precious.finance_tracker.entities.BankStatement;
import com.precious.finance_tracker.services.interfaces.IBankStatementService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bank-statements")
@RequiredArgsConstructor
@Tag(name = "Bank Statements")
@SecurityRequirement(name = "bearerAuth")
public class BankStatementController {
    private final IBankStatementService bankStatementService;

    @PostMapping()
    public ResponseEntity<BaseResponseDto<BankStatement>> addStatement(
            @RequestBody CreateStatementRequestDto dto
            ) {
        return ResponseEntity.
                status(HttpStatus.CREATED)
                .body(this.bankStatementService.addBankStatement(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BaseResponseDto<BankStatement>> updateStatement(
            @PathVariable("id") UUID id,
            @RequestBody UpdateStatementRequestDto dto
    ) {
        return ResponseEntity
                .ok(this.bankStatementService.updateBankStatements(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponseDto<BankStatement>> getStatement(
            @PathVariable("id") UUID id
    ) {
        return ResponseEntity.ok(this.bankStatementService.getBankStatement(id));
    }

    @GetMapping()
    public ResponseEntity<BaseResponseDto<PagedBankStatementResponseDto>> getAllStatements(
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit
    ) {

        return ResponseEntity.ok(
                this.bankStatementService.getAllStatements(page, limit)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponseDto<Object>> deleteStatement(
            @PathVariable("id") UUID id
    ) {
        return ResponseEntity.ok(
                this.bankStatementService.deleteBankStatement(id)
        );
    }

    @PostMapping("/{id}/analyse")
    public ResponseEntity<BaseResponseDto<BankStatement>> analyseBankStatements(
            @PathVariable("id") UUID id
    ) {
        return ResponseEntity.ok(
                this.bankStatementService.startAnalysis(id)
        );
    }
}
