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
import com.precious.finance_tracker.services.interfaces.ISummaryService;
import com.precious.finance_tracker.services.interfaces.IUserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SummaryService implements ISummaryService {
    private final static Logger log = LoggerFactory.getLogger(SummaryService.class.getName());

    private final IUserService userService;
    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final BudgetRepository budgetRepository;

    @Override
    public BaseResponseDto<CurrentMonthSummaryDto> getCurrentMonthSummary(YearMonth month) {
        User user = this.userService.getAuthenticatedUser();
        if (month == null) {
            month = YearMonth.now();
        }

        YearMonth lastMonth = month.minusMonths(1);

        BigDecimal currentIncome = incomeRepository.getTotalIncomeByMonth(user.getId(), month);
        BigDecimal lastIncome = incomeRepository.getTotalIncomeByMonth(user.getId(), lastMonth);

        BigDecimal currentExpense = expenseRepository.getTotalExpenseByMonth(user.getId(), month);
        BigDecimal lastExpense = expenseRepository.getTotalExpenseByMonth(user.getId(), lastMonth);

        BigDecimal currentBudget = budgetRepository.getTotalBudgetByMonth(user.getId(), month);
        BigDecimal lastBudget = budgetRepository.getTotalBudgetByMonth(user.getId(), lastMonth);

        CurrentMonthSummaryDto dto = CurrentMonthSummaryDto.builder()
                .totalIncome(currentIncome)
                .incomePercentageChange(calculatePercentageChange(currentIncome, lastIncome))
                .totalExpense(currentExpense)
                .expensePercentageChange(calculatePercentageChange(currentExpense, lastExpense))
                .totalBudget(currentBudget)
                .budgetPercentageChange(calculatePercentageChange(currentBudget, lastBudget))
                .build();

        log.info("Successfully fetched current month summary for user {}", user.getEmail());
        return BaseResponseDto.<CurrentMonthSummaryDto>builder()
                .status("Success")
                .message("Successfully fetched current month summary")
                .data(dto)
                .build();
    }

    private double calculatePercentageChange(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return (current != null && current.compareTo(BigDecimal.ZERO) > 0) ? 100.0 : 0.0;
        }
        if (current == null) {
            current = BigDecimal.ZERO;
        }
        return current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    @Override
    public BaseResponseDto<List<WeeklySummaryDto>> getWeeklyTotals(YearMonth month) {
        User user = this.userService.getAuthenticatedUser();
        if (month == null) {
            month = YearMonth.now();
        }

        List<Income> incomes = incomeRepository.findByUserAndDate(user.getId(), month);
        List<Expense> expenses = expenseRepository.findByUserAndDate(user.getId(), month);
        BigDecimal totalBudget = budgetRepository.getTotalBudgetByMonth(user.getId(), month);
        
        BigDecimal[] weeklyIncome = new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
        BigDecimal[] weeklyExpense = new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
        
        for (Income income : incomes) {
            int weekIndex = getWeekIndex(income.getTransactionDateTime());
            weeklyIncome[weekIndex] = weeklyIncome[weekIndex].add(income.getAmount());
        }
        
        for (Expense expense : expenses) {
            int weekIndex = getWeekIndex(expense.getTransactionDateTime());
            weeklyExpense[weekIndex] = weeklyExpense[weekIndex].add(expense.getAmount());
        }
        
        BigDecimal weeklyBudget = BigDecimal.ZERO;
        if (totalBudget != null && totalBudget.compareTo(BigDecimal.ZERO) > 0) {
            weeklyBudget = totalBudget.divide(BigDecimal.valueOf(4), 2, RoundingMode.HALF_UP);
        }
        
        List<WeeklySummaryDto> weeklySummaries = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            weeklySummaries.add(WeeklySummaryDto.builder()
                    .weekName("Week " + (i + 1))
                    .income(weeklyIncome[i])
                    .expense(weeklyExpense[i])
                    .budget(weeklyBudget)
                    .build());
        }
        
        log.info("Successfully fetched weekly totals for user {}", user.getEmail());
        return BaseResponseDto.<List<WeeklySummaryDto>>builder()
                .status("Success")
                .message("Successfully fetched weekly totals")
                .data(weeklySummaries)
                .build();
    }
    
    private int getWeekIndex(LocalDateTime dateTime) {
        if (dateTime == null) return 0; // Default to week 1 if no date
        int dayOfMonth = dateTime.getDayOfMonth();
        if (dayOfMonth <= 7) return 0;
        if (dayOfMonth <= 14) return 1;
        if (dayOfMonth <= 21) return 2;
        return 3;
    }
}
