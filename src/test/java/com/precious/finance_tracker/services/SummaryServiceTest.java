package com.precious.finance_tracker.services;

import com.precious.finance_tracker.dtos.BaseResponseDto;
import com.precious.finance_tracker.dtos.dashboard.CurrentMonthSummaryDto;
import com.precious.finance_tracker.dtos.dashboard.WeeklySummaryDto;
import com.precious.finance_tracker.entities.Expense;
import com.precious.finance_tracker.entities.Income;
import com.precious.finance_tracker.entities.User;
import com.precious.finance_tracker.repositories.BudgetRepository;
import com.precious.finance_tracker.repositories.ExpenseRepository;
import com.precious.finance_tracker.repositories.IncomeRepository;
import com.precious.finance_tracker.services.interfaces.IUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SummaryServiceTest {

    @Mock
    private IUserService userService;
    @Mock
    private IncomeRepository incomeRepository;
    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private BudgetRepository budgetRepository;

    @InjectMocks
    private SummaryService summaryService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(UUID.randomUUID())
                .name("John")
                .email("john@example.com")
                .build();
    }

    @Test
    void getCurrentMonthSummary_ShouldReturnSummary() {
        YearMonth month = YearMonth.now();
        YearMonth lastMonth = month.minusMonths(1);

        when(userService.getAuthenticatedUser()).thenReturn(mockUser);

        when(incomeRepository.getTotalIncomeByMonth(mockUser.getId(), month)).thenReturn(BigDecimal.valueOf(1000));
        when(incomeRepository.getTotalIncomeByMonth(mockUser.getId(), lastMonth)).thenReturn(BigDecimal.valueOf(500));

        when(expenseRepository.getTotalExpenseByMonth(mockUser.getId(), month)).thenReturn(BigDecimal.valueOf(400));
        when(expenseRepository.getTotalExpenseByMonth(mockUser.getId(), lastMonth)).thenReturn(BigDecimal.valueOf(200));

        when(budgetRepository.getTotalBudgetByMonth(mockUser.getId(), month)).thenReturn(BigDecimal.valueOf(500));
        when(budgetRepository.getTotalBudgetByMonth(mockUser.getId(), lastMonth)).thenReturn(BigDecimal.valueOf(500));

        BaseResponseDto<CurrentMonthSummaryDto> result = summaryService.getCurrentMonthSummary(month);

        assertEquals("Success", result.getStatus());
        CurrentMonthSummaryDto dto = result.getData();
        assertEquals(BigDecimal.valueOf(1000), dto.getTotalIncome());
        assertEquals(100.0, dto.getIncomePercentageChange());
        assertEquals(BigDecimal.valueOf(400), dto.getTotalExpense());
        assertEquals(100.0, dto.getExpensePercentageChange());
        assertEquals(BigDecimal.valueOf(500), dto.getTotalBudget());
        assertEquals(0.0, dto.getBudgetPercentageChange());
    }

    @Test
    void getCurrentMonthSummary_ShouldHandleNullMonthAndNullTotals() {
        when(userService.getAuthenticatedUser()).thenReturn(mockUser);

        // Without mocking specific values, mockito returns null for object returns
        when(incomeRepository.getTotalIncomeByMonth(any(), any())).thenReturn(null);
        when(expenseRepository.getTotalExpenseByMonth(any(), any())).thenReturn(null);
        when(budgetRepository.getTotalBudgetByMonth(any(), any())).thenReturn(null);

        BaseResponseDto<CurrentMonthSummaryDto> result = summaryService.getCurrentMonthSummary(null);

        assertEquals("Success", result.getStatus());
        CurrentMonthSummaryDto dto = result.getData();
        assertNull(dto.getTotalIncome());
        assertEquals(0.0, dto.getIncomePercentageChange());
    }

    @Test
    void getWeeklyTotals_ShouldReturnWeeklySummaries() {
        YearMonth month = YearMonth.now();
        when(userService.getAuthenticatedUser()).thenReturn(mockUser);

        Income incomeWeek1 = Income.builder()
                .amount(BigDecimal.valueOf(200))
                .transactionDateTime(LocalDateTime.now().withDayOfMonth(5))
                .build();
        
        Expense expenseWeek3 = Expense.builder()
                .amount(BigDecimal.valueOf(50))
                .transactionDateTime(LocalDateTime.now().withDayOfMonth(18))
                .build();

        when(incomeRepository.findAllByUserIdAndMonthOrderByTransactionDateTimeDesc(mockUser.getId(), month))
                .thenReturn(List.of(incomeWeek1));
        when(expenseRepository.findAllByUserIdAndMonthOrderByTransactionDateTimeDesc(mockUser.getId(), month))
                .thenReturn(List.of(expenseWeek3));
        when(budgetRepository.getTotalBudgetByMonth(mockUser.getId(), month))
                .thenReturn(BigDecimal.valueOf(400)); // 100 per week

        BaseResponseDto<List<WeeklySummaryDto>> result = summaryService.getWeeklyTotals(month);

        assertEquals("Success", result.getStatus());
        List<WeeklySummaryDto> dtos = result.getData();
        
        assertEquals(4, dtos.size());
        assertEquals(BigDecimal.valueOf(200), dtos.get(0).getIncome()); // Week 1 income
        assertEquals(BigDecimal.ZERO, dtos.get(0).getExpense());
        
        assertEquals(BigDecimal.ZERO, dtos.get(2).getIncome()); 
        assertEquals(BigDecimal.valueOf(50), dtos.get(2).getExpense()); // Week 3 expense

        for (WeeklySummaryDto dto : dtos) {
            assertEquals(0, BigDecimal.valueOf(100).compareTo(dto.getBudget())); // Week budget
        }
    }
}
