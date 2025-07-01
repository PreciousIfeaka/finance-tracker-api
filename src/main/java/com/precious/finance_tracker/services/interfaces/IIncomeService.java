package com.precious.finance_tracker.services.interfaces;

import com.precious.finance_tracker.dtos.BaseResponseDto;
import com.precious.finance_tracker.dtos.income.AddIncomeRequestDto;
import com.precious.finance_tracker.dtos.income.MonthlyIncomeStatsResponseDto;
import com.precious.finance_tracker.dtos.income.PagedIncomeResponseDto;
import com.precious.finance_tracker.dtos.income.UpdateIncomeRequestDto;
import com.precious.finance_tracker.entities.Income;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

public interface IIncomeService {
    BaseResponseDto<Income> addIncome(AddIncomeRequestDto dto);

    BaseResponseDto<Income> updateIncome(UUID id, UpdateIncomeRequestDto dto);

    BaseResponseDto<Income> getIncomeById(UUID id);

    BaseResponseDto<PagedIncomeResponseDto> getAllIncomesByMonth(
            int page, int limit, YearMonth month
    );

    BaseResponseDto<PagedIncomeResponseDto> getAllIncomes(int page, int limit);

    BaseResponseDto<Object> deleteIncomeById(UUID id);

    BaseResponseDto<List<MonthlyIncomeStatsResponseDto>> getMonthlyIncomeStats();
}
