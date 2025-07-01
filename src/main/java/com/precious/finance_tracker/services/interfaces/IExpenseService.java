package com.precious.finance_tracker.services.interfaces;

import com.precious.finance_tracker.dtos.BaseResponseDto;
import com.precious.finance_tracker.dtos.expense.AddExpenseRequestDto;
import com.precious.finance_tracker.dtos.expense.MonthlyExpenseStatsResponseDto;
import com.precious.finance_tracker.dtos.expense.PagedExpenseResponseDto;
import com.precious.finance_tracker.dtos.expense.UpdateExpenseRequestDto;
import com.precious.finance_tracker.entities.Expense;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

public interface IExpenseService {
    BaseResponseDto<Expense> addExpenseData(AddExpenseRequestDto dto);

    BaseResponseDto<Expense> updateExpense(UUID id, UpdateExpenseRequestDto dto);

    BaseResponseDto<Expense> getExpenseById(UUID id);

    BaseResponseDto<PagedExpenseResponseDto> getAllExpensesByMonth(
            int page, int limit, YearMonth month
    );

    BaseResponseDto<PagedExpenseResponseDto> getAllExpenses(int page, int limit);

    BaseResponseDto<Object> deleteExpenseById(UUID id);

    BaseResponseDto<List<MonthlyExpenseStatsResponseDto>> getMonthlyExpenseStats();
}
