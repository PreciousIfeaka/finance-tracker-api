package com.precious.finance_tracker.controllers;

import com.precious.finance_tracker.dtos.BaseResponseDto;
import com.precious.finance_tracker.dtos.income.AddIncomeRequestDto;
import com.precious.finance_tracker.entities.Income;
import com.precious.finance_tracker.services.IncomeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/income")
public class IncomeController {
    private IncomeService incomeService;

    @PostMapping("")
    public ResponseEntity<BaseResponseDto<Income>> addIncome(AddIncomeRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(this.incomeService.addIncome(dto));
    }
}
