package com.precious.finance_tracker.services;

import com.precious.finance_tracker.dtos.BaseResponseDto;
import com.precious.finance_tracker.dtos.expense.AddExpenseRequestDto;
import com.precious.finance_tracker.dtos.expense.MonthlyExpenseStatsResponseDto;
import com.precious.finance_tracker.dtos.expense.PagedExpenseResponseDto;
import com.precious.finance_tracker.dtos.expense.UpdateExpenseRequestDto;
import com.precious.finance_tracker.entities.Budget;
import com.precious.finance_tracker.entities.Expense;
import com.precious.finance_tracker.entities.User;
import com.precious.finance_tracker.exceptions.BadRequestException;
import com.precious.finance_tracker.exceptions.ConflictResourceException;
import com.precious.finance_tracker.exceptions.ForbiddenException;
import com.precious.finance_tracker.exceptions.NotFoundException;
import com.precious.finance_tracker.repositories.BudgetRepository;
import com.precious.finance_tracker.repositories.ExpenseRepository;
import com.precious.finance_tracker.repositories.UserRepository;
import com.precious.finance_tracker.services.interfaces.IExpenseService;
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
public class ExpenseService implements IExpenseService {
    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final IUserService userService;
    private final BudgetRepository budgetRepository;

    @Transactional
    public BaseResponseDto<Expense> addExpenseData(AddExpenseRequestDto dto) {
        User user = this.userService.getAuthenticatedUser();
        YearMonth currentMonth = YearMonth.now();

        if (user.getCurrency() == null) {
            throw new BadRequestException("Currency has not been set in user profile");
        }

        Optional<Expense> existingExpense = this.expenseRepository.findRecurringExpense(
                user.getId(), dto.getAmount(), dto.getCategory()
        );

        if (existingExpense.isPresent()) {
            long minutesSinceLast = Math.abs(
                    LocalDateTime.now().getMinute() - existingExpense.get().getCreatedAt().getMinute()
            );
            if (minutesSinceLast < 2) {
                throw new ConflictResourceException("Duplicate expense entry, try again in 2 mins");
            }
        }

        Optional<Budget> categoryBudget = this.budgetRepository.findByUserAndCategoryAndDate(
                        user.getId(), currentMonth, dto.getCategory()
        );

        String budgetWarnMessage = "";

        if (categoryBudget.isPresent()) {
            Budget budget = categoryBudget.get();
            if (dto.getAmount().compareTo(budget.getAmount()) > 0) {
                budgetWarnMessage = dto.getCategory() + " budget is exceeded";
                budget.setIsExceeded(true);
                budgetRepository.save(budget);
            }
        }

        Expense expense = Expense.builder()
                .amount(dto.getAmount())
                .note(dto.getNote())
                .category(dto.getCategory())
                .month(currentMonth)
                .isRecurring(dto.getIsRecurring())
                .user(user)
                .build();

        return BaseResponseDto.<Expense>builder()
                .status("Success")
                .message("Successfully added expense" + (budgetWarnMessage.isEmpty() ? "" : ", " + budgetWarnMessage))
                .data(this.expenseRepository.save(expense))
                .build();
    }

    public BaseResponseDto<Expense> updateExpense(UUID id, UpdateExpenseRequestDto dto) {
        User user = this.userService.getAuthenticatedUser();

        Expense expense = this.expenseRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException("Expense with given ID not found"));

        if (!user.getExpenses().contains(expense)) {
            throw new ForbiddenException("Forbidden access to specified expense");
        }

        String budgetWarnMessage = "";

        Optional<Budget> categoryBudget =
                this.budgetRepository.findByUserAndCategoryAndDate(
                        user.getId(), expense.getMonth(), expense.getCategory()
                );
        if (
                categoryBudget.isPresent() &&
                        categoryBudget.get().getAmount().compareTo(dto.getAmount()) < 0
        ) {
            budgetWarnMessage = dto.getCategory() + " budget is exceeded";

            categoryBudget.get().setIsExceeded(true);
            this.budgetRepository.save(categoryBudget.get());
        }

        if (dto.getAmount() != null) expense.setAmount(dto.getAmount());
        if (dto.getNote() != null) expense.setNote(dto.getNote());
        if (dto.getCategory() != null) expense.setCategory(dto.getCategory());
        if (dto.getIsRecurring() != expense.getIsRecurring()) expense.setIsRecurring(dto.getIsRecurring());

        return BaseResponseDto.<Expense>builder()
                .message("Successfully updated expense data but, " + budgetWarnMessage)
                .status("Success")
                .data(this.expenseRepository.save(expense))
                .build();
    }

    public BaseResponseDto<Expense> getExpenseById(UUID id) {
        User user = this.userService.getAuthenticatedUser();

        Expense expense = this.expenseRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException("Expense not found"));

        if (!expense.getUser().getEmail().equals(user.getEmail())) {
            throw new ForbiddenException("Forbidden access to this expense data");
        }

        return BaseResponseDto.<Expense>builder()
                .status("Success")
                .message("Successfully retrieved expense")
                .data(expense)
                .build();
    }

    public BaseResponseDto<PagedExpenseResponseDto> getAllExpensesByMonth(
            int page, int limit, YearMonth month
    ) {
        User user = this.userService.getAuthenticatedUser();

        Page<Expense> expenses = this.expenseRepository.findByUserAndDate(
                user.getId(), month, PageRequest.of(page - 1, limit)
        );

        return BaseResponseDto.<PagedExpenseResponseDto>builder()
                .status("Success")
                .message("Successfully retrieved expenses")
                .data(new PagedExpenseResponseDto(expenses, this.getTotalExpenseByMonth(month)))
                .build();
    }

    public BaseResponseDto<PagedExpenseResponseDto> getAllExpenses(int page, int limit) {
        User user = this.userService.getAuthenticatedUser();

        Page<Expense> expenses = this.expenseRepository
                .findByUserIdAndDeletedAtIsNull(user.getId(), PageRequest.of(page - 1, limit));

        BigDecimal totalExpenses = this.expenseRepository.sumExpense(user.getId());

        return BaseResponseDto.<PagedExpenseResponseDto>builder()
                .status("Success")
                .message("Successfully retrieved all expenses")
                .data(new PagedExpenseResponseDto(expenses, totalExpenses))
                .build();
    }

    @Transactional
    public BaseResponseDto<Object> deleteExpenseById(UUID id) {
        User user = this.userService.getAuthenticatedUser();

        Expense expense = this.getExpenseById(id).getData();

        expense.setDeletedAt(LocalDateTime.now());

        Optional<Budget> categoryBudget =
                this.budgetRepository.findByUserAndCategoryAndDate(
                        user.getId(), expense.getMonth(), expense.getCategory()
                );
        if (
                categoryBudget.isPresent() &&
                        categoryBudget.get().getAmount().compareTo(expense.getAmount()) > 0
        ) {

            categoryBudget.get().setIsExceeded(false);
            this.budgetRepository.save(categoryBudget.get());
        }

        this.expenseRepository.save(expense);

        return BaseResponseDto.builder()
                .status("Success")
                .message("Successfully deleted expense with ID " + expense.getId())
                .data(null)
                .build();
    }

    public BaseResponseDto<List<MonthlyExpenseStatsResponseDto>> getMonthlyExpenseStats() {
        User user = this.userService.getAuthenticatedUser();

        List<Object[]> results = this.expenseRepository.sumExpenseByMonth(user.getId());

        List<MonthlyExpenseStatsResponseDto> stats = results.stream()
                .map(row -> new MonthlyExpenseStatsResponseDto(
                        (YearMonth) row[0],
                        (BigDecimal) row[1]
                ))
                .toList();

        return BaseResponseDto.<List<MonthlyExpenseStatsResponseDto>>builder()
                .status("Success")
                .message("Successfully retrieved monthly expense stats")
                .data(stats)
                .build();
    }

    private BigDecimal getTotalExpenseByMonth(YearMonth month) {
        User user = this.userService.getAuthenticatedUser();

        return this.expenseRepository.getTotalExpenseByMonth(
                 user.getId(), month
        );
    }
}
