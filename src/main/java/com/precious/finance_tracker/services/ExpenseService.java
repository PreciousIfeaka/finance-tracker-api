package com.precious.finance_tracker.services;

import com.precious.finance_tracker.dtos.BaseResponseDto;
import com.precious.finance_tracker.dtos.budget.DeleteByIdsDto;
import com.precious.finance_tracker.dtos.expense.AddExpenseRequestDto;
import com.precious.finance_tracker.dtos.expense.MonthlyExpenseStatsResponseDto;
import com.precious.finance_tracker.dtos.expense.PagedExpenseResponseDto;
import com.precious.finance_tracker.dtos.expense.UpdateExpenseRequestDto;
import com.precious.finance_tracker.dtos.transactions.CreateTransactionDto;
import com.precious.finance_tracker.entities.Budget;
import com.precious.finance_tracker.entities.Expense;
import com.precious.finance_tracker.entities.Transactions;
import com.precious.finance_tracker.entities.User;
import com.precious.finance_tracker.enums.ExpenseCategory;
import com.precious.finance_tracker.enums.TransactionDirection;
import com.precious.finance_tracker.exceptions.ForbiddenException;
import com.precious.finance_tracker.exceptions.NotFoundException;
import com.precious.finance_tracker.repositories.BudgetRepository;
import com.precious.finance_tracker.repositories.ExpenseRepository;
import com.precious.finance_tracker.repositories.TransactionRepository;
import com.precious.finance_tracker.repositories.UserRepository;
import com.precious.finance_tracker.services.interfaces.IExpenseService;
import com.precious.finance_tracker.services.interfaces.IUserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ExpenseService implements IExpenseService {
    private final Logger log = LoggerFactory.getLogger(ExpenseService.class.getName());

    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final IUserService userService;
    private final BudgetRepository budgetRepository;
    private final TransactionsService transactionsService;
    private final TransactionRepository transactionRepository;

    @Transactional
    public BaseResponseDto<Expense> addExpenseData(AddExpenseRequestDto dto) {
        User user = this.userService.getAuthenticatedUser();

        YearMonth currentMonth = dto.getTransactionDateTime() != null
                ? YearMonth.from(dto.getTransactionDateTime())
                : YearMonth.now();

        Optional<Budget> categoryBudget = this.budgetRepository.findByUserIdAndMonthAndCategory(
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
                .transactionDateTime(dto.getTransactionDateTime())
                .isRecurring(dto.getIsRecurring())
                .user(user)
                .build();

        this.transactionsService.createTransaction(
                CreateTransactionDto.builder()
                        .amount(dto.getAmount())
                        .description(dto.getNote())
                        .transactionDateTime(dto.getTransactionDateTime())
                        .direction(TransactionDirection.debit)
                        .build()
        );

        log.info("Successfully added expense record for user {}", user.getEmail());
        return BaseResponseDto.<Expense>builder()
                .status("Success")
                .message("Successfully added expense" + (budgetWarnMessage.isEmpty() ? "" : ", " + budgetWarnMessage))
                .data(this.expenseRepository.save(expense))
                .build();
    }

    @Transactional
    public BaseResponseDto<Expense> updateExpense(UUID id, UpdateExpenseRequestDto dto) {
        User user = this.userService.getAuthenticatedUser();

        Expense expense = this.expenseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Expense with given ID not found"));

        if (!user.getExpenses().contains(expense)) {
            throw new ForbiddenException("Forbidden access to specified expense");
        }

        String budgetWarnMessage = "";

        Optional<Budget> categoryBudget =
                this.budgetRepository.findByUserIdAndMonthAndCategory(
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

        Optional<Transactions> transaction = this.transactionRepository
                .findByUserIdAndAmountAndDirectionAndTransactionDateTimeAndDescription(
                        user.getId(),
                        expense.getAmount(),
                        TransactionDirection.debit,
                        expense.getTransactionDateTime(),
                        expense.getNote()
                );

        if (dto.getAmount() != null) expense.setAmount(dto.getAmount());
        if (dto.getNote() != null) expense.setNote(dto.getNote());
        if (dto.getCategory() != null) expense.setCategory(dto.getCategory());
        if (dto.getIsRecurring() != null) expense.setIsRecurring(dto.getIsRecurring());
        if (dto.getTransactionDateTime() != null) {
            expense.setMonth(YearMonth.from(dto.getTransactionDateTime()));
            expense.setTransactionDateTime(dto.getTransactionDateTime());
        }

        if (transaction.isPresent()) {
            Transactions t = transaction.get();
            t.setAmount(dto.getAmount() != null ? dto.getAmount() : t.getAmount());
            t.setMonth(
                    dto.getTransactionDateTime() != null
                            ? YearMonth.from(dto.getTransactionDateTime())
                            : t.getMonth()
            );
            t.setTransactionDateTime(
                    dto.getTransactionDateTime() != null
                            ? dto.getTransactionDateTime()
                            : t.getTransactionDateTime()
            );
            t.setDescription(dto.getNote() != null ? dto.getNote() : t.getDescription());

            this.transactionRepository.save(t);
        }

        log.info("Successfully updated expense record for user {}", user.getEmail());
        return BaseResponseDto.<Expense>builder()
                .message("Successfully updated expense data. " + budgetWarnMessage)
                .status("Success")
                .data(this.expenseRepository.save(expense))
                .build();
    }

    public BaseResponseDto<Expense> getExpenseById(UUID id) {
        User user = this.userService.getAuthenticatedUser();

        Expense expense = this.expenseRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new NotFoundException("Expense not found"));

        log.info("Successfully retrieved expense for user {}", user.getEmail());
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

        Page<Expense> expenses = this.expenseRepository.findAllByUserIdAndMonthOrderByCreatedAtDesc(
                user.getId(), month, PageRequest.of(page - 1, limit)
        );

        log.info("Successfully retrieved {} expenses for user {}", month, user.getEmail());
        return BaseResponseDto.<PagedExpenseResponseDto>builder()
                .status("Success")
                .message("Successfully retrieved expenses")
                .data(new PagedExpenseResponseDto(expenses, this.getTotalExpenseByMonth(user.getId(), month)))
                .build();
    }

    public BaseResponseDto<PagedExpenseResponseDto> getAllExpensesByMonthAndCategory(
            int page, int limit, YearMonth month, ExpenseCategory category
    ) {
        User user = this.userService.getAuthenticatedUser();

        Page<Expense> expenses = this.expenseRepository.findAllByUserIdAndMonthAndCategoryOrderByCreatedAtDesc(
                user.getId(), month, category, PageRequest.of(page - 1, limit)
        );

        log.info(
                "Successfully retrieved {} expenses for month {} and user {}",
                category, month, user.getEmail()
        );
        return BaseResponseDto.<PagedExpenseResponseDto>builder()
                .status("Success")
                .message("Successfully retrieved expenses")
                .data(new PagedExpenseResponseDto(
                        expenses,
                        this.expenseRepository.sumExpenseByUserIdAndMonthAndCategory(
                                user.getId(), month, category
                        ))
                )
                .build();
    }

    public BaseResponseDto<PagedExpenseResponseDto> getAllExpenses(int page, int limit) {
        User user = this.userService.getAuthenticatedUser();

        Page<Expense> expenses = this.expenseRepository
                .findAllByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(page - 1, limit));

        BigDecimal totalExpenses = this.expenseRepository.sumExpense(user.getId());

        log.info("Successfully retrieved expenses for user {}", user.getEmail());
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

        Optional<Budget> categoryBudget =
                this.budgetRepository.findByUserIdAndMonthAndCategory(
                        user.getId(), expense.getMonth(), expense.getCategory()
                );
        if (
                categoryBudget.isPresent() &&
                        categoryBudget.get().getAmount().compareTo(expense.getAmount()) > 0
        ) {

            categoryBudget.get().setIsExceeded(false);
            this.budgetRepository.save(categoryBudget.get());
        }

        this.expenseRepository.deleteById(expense.getId());

        log.info("Successfully deleted expenses for user {}", user.getEmail());
        return BaseResponseDto.builder()
                .status("Success")
                .message("Successfully deleted expense")
                .data(null)
                .build();
    }

    @Transactional
    public BaseResponseDto<Object> deleteExpensesByIds(DeleteByIdsDto dto) {
        this.expenseRepository.deleteAllById(dto.getIds());

        log.info("Successfully deleted selected expenses");

        return BaseResponseDto.builder()
                .status("Success")
                .message("Successfully deleted selected expenses")
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

        log.info("Successfully retrieved monthly expenses stats for user {}", user.getEmail());
        return BaseResponseDto.<List<MonthlyExpenseStatsResponseDto>>builder()
                .status("Success")
                .message("Successfully retrieved monthly expense stats")
                .data(stats)
                .build();
    }

    public BigDecimal getTotalExpenseByMonth(UUID userId, YearMonth month) {
        return this.expenseRepository.getTotalExpenseByMonth(
                 userId, month
        );
    }
}
