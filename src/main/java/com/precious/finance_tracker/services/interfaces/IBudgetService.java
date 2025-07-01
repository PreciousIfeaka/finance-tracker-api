package com.precious.finance_tracker.services.interfaces;

import com.precious.finance_tracker.dtos.BaseResponseDto;
import com.precious.finance_tracker.dtos.budget.*;
import com.precious.finance_tracker.entities.Budget;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

public interface IBudgetService {
    BaseResponseDto<Budget> createBudget(CreateBudgetRequestDto dto);

    BaseResponseDto<Budget> updateBudget(UUID id, UpdateBudgetRequestDto dto);

    BaseResponseDto<Budget> getBudget(UUID id);

    BaseResponseDto<PagedBudgetResponseDto> getAllBudgets(int page, int limit);

    BaseResponseDto<PagedBudgetResponseDto> getAllBudgetsByMonth(
            int page, int limit, YearMonth month
    );

    BaseResponseDto<Object> deleteBudgetById(UUID id);

    BaseResponseDto<List<MonthlyBudgetStatsResponseDto>> getMonthlyBudgetStats();
}
