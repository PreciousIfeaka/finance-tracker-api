package com.precious.finance_tracker.services;

import com.precious.finance_tracker.dtos.BaseResponseDto;
import com.precious.finance_tracker.dtos.expense.AddExpenseRequestDto;
import com.precious.finance_tracker.dtos.expense.MonthlyExpenseStatsResponseDto;
import com.precious.finance_tracker.dtos.expense.PagedExpenseResponseDto;
import com.precious.finance_tracker.dtos.expense.UpdateExpenseRequestDto;
import com.precious.finance_tracker.entities.Expense;
import com.precious.finance_tracker.entities.User;
import com.precious.finance_tracker.exceptions.BadRequestException;
import com.precious.finance_tracker.exceptions.ConflictResourceException;
import com.precious.finance_tracker.exceptions.ForbiddenException;
import com.precious.finance_tracker.exceptions.NotFoundException;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Data
public class ExpenseService implements IExpenseService {
    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final IUserService userService;

    public BaseResponseDto<Expense> addExpenseData(AddExpenseRequestDto dto) {
        User user = this.userService.getAuthenticatedUser();

        Optional<Expense> existingExpense = this.expenseRepository.findRecurringExpense(
                user.getId(), dto.getDueDate(), dto.getAmount(), dto.getCategory()
        );

        if (existingExpense.isPresent()) {
            throw new ConflictResourceException("Similar expense entry already exists.");
        } else if (user.getCurrency() == null) {
            throw new BadRequestException("Currency has not been set in user profile");
        } else if (dto.getAmount().compareTo(BigDecimal.valueOf(10)) < 0) {
            throw new BadRequestException("Minimum amount is " + user.getCurrency() + "10.");
        }

        Expense expense = Expense.builder()
                .amount(dto.getAmount())
                .note(dto.getNote())
                .category(dto.getCategory())
                .dueDate(dto.getDueDate())
                .isRecurring(false)
                .user(user)
                .build();

        return BaseResponseDto.<Expense>builder()
                .status("Success")
                .message("Successfully added expense")
                .data(this.expenseRepository.save(expense))
                .build();
    }

    public BaseResponseDto<Expense> updateExpense(UUID id, UpdateExpenseRequestDto dto) {
        User user = this.userService.getAuthenticatedUser();

        if (dto.getAmount() != null && dto.getAmount().compareTo(BigDecimal.valueOf(10)) < 0) {
            throw new BadRequestException("Minimum amount is " + user.getCurrency() + "10.");
        }

        Expense expense = this.expenseRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException("Expense with given ID not found"));

        if (!user.getExpenses().contains(expense)) {
            throw new ForbiddenException("Forbidden access to specified expense");
        }

        if (dto.getAmount() != null) expense.setAmount(dto.getAmount());
        if (dto.getNote() != null) expense.setNote(dto.getNote());
        if (dto.getCategory() != null) expense.setCategory(dto.getCategory());
        if (dto.getDueDate() != null) expense.setDueDate(dto.getDueDate());

        return BaseResponseDto.<Expense>builder()
                .message("Successfully updated expense data")
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
            int page, int limit, LocalDate date
    ) {
        User user = this.userService.getAuthenticatedUser();

        Page<Expense> expenses = this.expenseRepository.findByUserAndDate(
                user.getId(), date.getYear(), date.getMonthValue(), PageRequest.of(page, limit)
        );

        return BaseResponseDto.<PagedExpenseResponseDto>builder()
                .status("Success")
                .message("Successfully retrieved expenses")
                .data(new PagedExpenseResponseDto(expenses))
                .build();
    }

    public BaseResponseDto<PagedExpenseResponseDto> getAllExpenses(int page, int limit) {
        User user = this.userService.getAuthenticatedUser();

        Page<Expense> expenses = this.expenseRepository
                .findByUserIdAndDeletedAtIsNull(user.getId(), PageRequest.of(page, limit));

        return BaseResponseDto.<PagedExpenseResponseDto>builder()
                .status("Success")
                .message("Successfully retrieved all expenses")
                .data(new PagedExpenseResponseDto(expenses))
                .build();
    }

    @Transactional
    public BaseResponseDto<Object> deleteExpenseById(UUID id) {
        Expense expense = this.getExpenseById(id).getData();

        expense.setDeletedAt(LocalDateTime.now());

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
                        (String) row[0],
                        (BigDecimal) row[1]
                ))
                .toList();

        return BaseResponseDto.<List<MonthlyExpenseStatsResponseDto>>builder()
                .status("Success")
                .message("Successfully retrieved monthly expense stats")
                .data(stats)
                .build();

    }
}
