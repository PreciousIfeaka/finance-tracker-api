package com.precious.finance_tracker.services;

import com.precious.finance_tracker.dtos.BaseResponseDto;
import com.precious.finance_tracker.dtos.budget.*;
import com.precious.finance_tracker.entities.Budget;
import com.precious.finance_tracker.entities.Expense;
import com.precious.finance_tracker.entities.Income;
import com.precious.finance_tracker.entities.User;
import com.precious.finance_tracker.enums.ExpenseCategory;
import com.precious.finance_tracker.exceptions.BadRequestException;
import com.precious.finance_tracker.exceptions.ConflictResourceException;
import com.precious.finance_tracker.exceptions.NotFoundException;
import com.precious.finance_tracker.repositories.BudgetRepository;
import com.precious.finance_tracker.repositories.ExpenseRepository;
import com.precious.finance_tracker.repositories.IncomeRepository;
import com.precious.finance_tracker.services.interfaces.IBudgetService;
import com.precious.finance_tracker.services.interfaces.IIncomeService;
import com.precious.finance_tracker.services.interfaces.IUserService;
import lombok.Data;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

@Service
@Data
public class BudgetService implements IBudgetService {
    private final IUserService userService;
    private final IIncomeService incomeService;
    private final IncomeRepository incomeRepository;
    private final BudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;

    @Transactional
    public BaseResponseDto<Budget> createBudget(CreateBudgetRequestDto dto) {
        User user = userService.getAuthenticatedUser();
        YearMonth currentMonth = YearMonth.now();

        List<Income> currentMonthIncomes = this.incomeRepository.findByUserAndDate(user.getId(), currentMonth);

        if (currentMonthIncomes.isEmpty()) {
            throw new BadRequestException("An income for the month has to be added");
        }

        BigDecimal totalMonthIncome = this.incomeRepository.getTotalIncomeByMonth(user.getId(), currentMonth);

        BigDecimal currentTotalBudget = this.budgetRepository.getTotalBudgetByMonth(user.getId(), currentMonth);
        BigDecimal projectedTotalBudget = currentTotalBudget.add(dto.amount());

        if (totalMonthIncome.compareTo(projectedTotalBudget) < 0) {
            throw new BadRequestException("Budget cannot be more than income per month");
        }

        boolean categoryAlreadyBudgeted = this.budgetRepository
                .findByUserAndCategoryAndDate(user.getId(), currentMonth, dto.category())
                .isPresent();

        if (categoryAlreadyBudgeted) {
            throw new ConflictResourceException("Same-category budget cannot be created in the same month");
        }

        boolean allCategoryBudgetExists = this.budgetRepository
                .findByUserAndCategoryAndDate(user.getId(), currentMonth, ExpenseCategory.all)
                .isPresent();

        if (allCategoryBudgetExists) {
            throw new BadRequestException("Cannot create a new budget, an all-category budget is present");
        }

        Long totalMonthBudgets = this.getAllBudgetsByMonth(1, 10, currentMonth).getData().getTotal();

        if (totalMonthBudgets > 0 && dto.category() == ExpenseCategory.all) {
            throw new BadRequestException("An 'all-categories' budget cannot be added when other budgets exist for this month.");
        }

        Optional<Budget> existingBudget = this.budgetRepository.findRecurringBudget(
                user.getId(), dto.amount(), dto.category()
        );

        if (existingBudget.isPresent()) {
            long minuteDifference = Math.abs(
                    LocalDateTime.now().getMinute() - existingBudget.get().getCreatedAt().getMinute()
            );
            if (minuteDifference < 2) {
                throw new ConflictResourceException("Duplicate budget entry, try again in 2 mins if valid");
            }
        }

        Optional<Expense> expenseForCategory =
                this.expenseRepository.findByUserAndCategoryAndDate(
                        user.getId(), currentMonth, dto.category()
                );

        boolean budgetExceeded = false;
        if (expenseForCategory.isPresent() &&
                expenseForCategory.get().getAmount().compareTo(dto.amount()) > 0
        ) {
            budgetExceeded = true;
        }

        Budget budget = Budget.builder()
                .amount(dto.amount())
                .category(dto.category())
                .isRecurring(dto.isRecurring())
                .month(currentMonth)
                .isExceeded(budgetExceeded)
                .user(user)
                .build();

        Budget savedBudget = this.budgetRepository.save(budget);

        return BaseResponseDto.<Budget>builder()
                .status("Success")
                .message("Successfully created a budget")
                .data(savedBudget)
                .build();
    }

    public BaseResponseDto<Budget> getBudget(UUID id) {
        Budget budget = this.budgetRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException("Budget not found"));

        return BaseResponseDto.<Budget>builder()
                .status("Success")
                .message("Successfully retrieved budget")
                .data(budget)
                .build();
    }

    public BaseResponseDto<PagedBudgetResponseDto> getAllBudgets(int page, int limit) {
        User user = this.userService.getAuthenticatedUser();

        Page<Budget> budgets =
                this.budgetRepository.findAllByUserIdAndDeletedAtIsNull(
                        user.getId(), PageRequest.of(page, limit)
                );

        BigDecimal totalBudget = this.budgetRepository.sumBudget(user.getId());
        return BaseResponseDto.<PagedBudgetResponseDto>builder()
                .status("Success")
                .message("Successfully retrieved all budgets")
                .data(new PagedBudgetResponseDto(budgets, totalBudget))
                .build();
    }

    @Transactional
    public BaseResponseDto<Budget> updateBudget(UUID id, UpdateBudgetRequestDto dto) {
        Budget budget = this.getBudget(id).getData();

        BigDecimal totalMonthIncome =
                this.incomeRepository.getTotalIncomeByMonth(
                        budget.getUser().getId(), budget.getMonth()
                );

        BigDecimal currentTotalBudget =
                this.budgetRepository.getTotalBudgetByMonth(
                        budget.getUser().getId(), budget.getMonth()
                );
        BigDecimal projectedTotalBudget = currentTotalBudget.add(dto.amount());

        if (totalMonthIncome.compareTo(projectedTotalBudget) < 0) {
            throw new BadRequestException("Budget cannot be more than income per month");
        }

        Optional<Expense> expenseForCategory =
                this.expenseRepository.findByUserAndCategoryAndDate(
                        budget.getUser().getId(), budget.getMonth(), budget.getCategory()
                );

        if (dto.amount() != null) budget.setAmount(dto.amount());
        if (dto.category() != null) budget.setCategory(dto.category());
        if (dto.isRecurring() != budget.getIsRecurring()) budget.setIsRecurring(dto.isRecurring());

        if (expenseForCategory.isPresent() &&
                expenseForCategory.get().getAmount().compareTo(budget.getAmount()) > 0
        ) {
            budget.setIsExceeded(true);
        }

        this.budgetRepository.save(budget);

        return BaseResponseDto.<Budget>builder()
                .status("Success")
                .message("Successfully updated budget")
                .data(budget)
                .build();
    }

    public BaseResponseDto<PagedBudgetResponseDto> getAllBudgetsByMonth(
            int page, int limit, YearMonth month
    ) {
        User user = this.userService.getAuthenticatedUser();

        Page<Budget> budgets = this.budgetRepository.findByUserAndDate(
                user.getId(), month, PageRequest.of(page, limit)
        );

        return BaseResponseDto.<PagedBudgetResponseDto>builder()
                .status("Success")
                .message("Successfully retrieved budgets for " + month)
                .data(new PagedBudgetResponseDto(budgets, this.getTotalBudgetByMonth(month)))
                .build();
    }

    @Transactional
    public BaseResponseDto<Object> deleteBudgetById(UUID id) {
        Budget budget = this.getBudget(id).getData();

        budget.setDeletedAt(LocalDateTime.now());

        this.budgetRepository.save(budget);

        return BaseResponseDto.builder()
                .status("Success")
                .message("Successfully deleted budget with ID " + budget.getId())
                .data(null)
                .build();
    }

    public BaseResponseDto<List<MonthlyBudgetStatsResponseDto>> getMonthlyBudgetStats() {
        User user = this.userService.getAuthenticatedUser();

        List<Object[]> results = this.budgetRepository.sumBudgetByMonth(user.getId());

        List<MonthlyBudgetStatsResponseDto> stats = results.stream()
                .map(row -> new MonthlyBudgetStatsResponseDto(
                        (YearMonth) row[0],
                        (BigDecimal) row[1]
                ))
                .toList();

        return BaseResponseDto.<List<MonthlyBudgetStatsResponseDto>>builder()
                .status("Success")
                .message("Successfully retrieved monthly budget stats")
                .data(stats)
                .build();
    }

    private BigDecimal getTotalBudgetByMonth(YearMonth month) {
        User user = this.userService.getAuthenticatedUser();

        return this.budgetRepository.getTotalBudgetByMonth(
                        user.getId(), month
                );
    }
}

// To Do
// Check budget-expense relationship
// If there is a budget for an expense, track both, if not, allow.
