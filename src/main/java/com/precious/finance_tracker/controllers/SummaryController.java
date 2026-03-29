package com.precious.finance_tracker.controllers;

import com.precious.finance_tracker.dtos.BaseResponseDto;
import com.precious.finance_tracker.dtos.dashboard.CurrentMonthSummaryDto;
import com.precious.finance_tracker.dtos.dashboard.WeeklySummaryDto;
import com.precious.finance_tracker.services.interfaces.ISummaryService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/v1/summary")
@RequiredArgsConstructor
@Tag(name = "Summary / Dashboard")
@SecurityRequirement(name = "bearerAuth")
public class SummaryController {
    private final ISummaryService summaryService;

    @GetMapping("/current-month")
    public ResponseEntity<BaseResponseDto<CurrentMonthSummaryDto>> getCurrentMonthSummary(
            @RequestParam(required = false, value = "month") @DateTimeFormat(pattern = "yyyy-MM") YearMonth month
    ) {
        return ResponseEntity.ok(this.summaryService.getCurrentMonthSummary(month));
    }

    @GetMapping("/weekly-totals")
    public ResponseEntity<BaseResponseDto<List<WeeklySummaryDto>>> getWeeklyTotals(
            @RequestParam(required = false, value = "month") @DateTimeFormat(pattern = "yyyy-MM") YearMonth month
    ) {
        return ResponseEntity.ok(this.summaryService.getWeeklyTotals(month));
    }
}
