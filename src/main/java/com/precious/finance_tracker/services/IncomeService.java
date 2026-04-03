package com.precious.finance_tracker.services;

import com.precious.finance_tracker.dtos.BaseResponseDto;
import com.precious.finance_tracker.dtos.budget.DeleteByIdsDto;
import com.precious.finance_tracker.dtos.income.AddIncomeRequestDto;
import com.precious.finance_tracker.dtos.income.MonthlyIncomeStatsResponseDto;
import com.precious.finance_tracker.dtos.income.PagedIncomeResponseDto;
import com.precious.finance_tracker.dtos.income.UpdateIncomeRequestDto;
import com.precious.finance_tracker.dtos.transactions.CreateTransactionDto;
import com.precious.finance_tracker.entities.Income;
import com.precious.finance_tracker.entities.Transactions;
import com.precious.finance_tracker.entities.User;
import com.precious.finance_tracker.enums.TransactionDirection;
import com.precious.finance_tracker.exceptions.BadRequestException;
import com.precious.finance_tracker.exceptions.ConflictResourceException;
import com.precious.finance_tracker.exceptions.ForbiddenException;
import com.precious.finance_tracker.exceptions.NotFoundException;
import com.precious.finance_tracker.repositories.IncomeRepository;
import com.precious.finance_tracker.repositories.TransactionRepository;
import com.precious.finance_tracker.services.interfaces.IIncomeService;
import com.precious.finance_tracker.services.interfaces.ITransactionService;
import com.precious.finance_tracker.services.interfaces.IUserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncomeService implements IIncomeService {
    private final IncomeRepository incomeRepository;
    private final IUserService userService;
    private final ITransactionService transactionsService;
    private final TransactionRepository transactionRepository;

    @Transactional
    public BaseResponseDto<Income> addIncome(AddIncomeRequestDto dto) {
        User user = this.userService.getAuthenticatedUser();

        Income income = Income.builder()
                .amount(dto.getAmount())
                .note(dto.getNote())
                .source(dto.getSource())
                .transactionDateTime(dto.getTransactionDateTime())
                .month(
                        dto.getTransactionDateTime() != null
                        ? YearMonth.from(dto.getTransactionDateTime())
                        : YearMonth.now()
                )
                .isRecurring(dto.getIsRecurring())
                .user(user)
                .build();

        log.info("Successfully added income record for user {}", user.getEmail());
        this.transactionsService.createTransaction(
                CreateTransactionDto.builder()
                        .amount(dto.getAmount())
                        .description(dto.getNote())
                        .transactionDateTime(dto.getTransactionDateTime())
                        .direction(TransactionDirection.credit)
                        .build()
        );

        return BaseResponseDto.<Income>builder()
                .status("Success")
                .message("Successfully added income")
                .data(this.incomeRepository.save(income))
                .build();
    }

    @Transactional
    public BaseResponseDto<Income> updateIncome(UUID id, UpdateIncomeRequestDto dto) {
        User user = this.userService.getAuthenticatedUser();

        Income income = this.incomeRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new NotFoundException("Income with given ID not found"));


        if (dto.getAmount() != null) income.setAmount(dto.getAmount());
        if (dto.getNote() != null) income.setNote(dto.getNote());
        if (dto.getSource() != null) income.setSource(dto.getSource());
        if (dto.getIsRecurring() != income.getIsRecurring()) income.setIsRecurring(dto.getIsRecurring());
        if (dto.getTransactionDateTime() != null) {
            income.setMonth(YearMonth.from(dto.getTransactionDateTime()));
            income.setTransactionDateTime(dto.getTransactionDateTime());
        }

        Optional<Transactions> transaction = this.transactionRepository
                .findByUserIdAndAmountAndDirectionAndTransactionDateTimeAndDescription(
                        user.getId(),
                        income.getAmount(),
                        TransactionDirection.credit,
                        income.getTransactionDateTime(),
                        income.getNote()
                );

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

        log.info("Successfully updated income record for user {}", user.getEmail());
        return BaseResponseDto.<Income>builder()
                .message("Successfully updated income data")
                .status("Success")
                .data(this.incomeRepository.save(income))
                .build();
    }

    public BaseResponseDto<Income> getIncomeById(UUID id) {
        User user = this.userService.getAuthenticatedUser();

        Income income = this.incomeRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new NotFoundException("Income not found"));

        log.info("Successfully retrieved income for user {}", user.getEmail());
        return BaseResponseDto.<Income>builder()
                .status("Success")
                .message("Successfully retrieved income")
                .data(income)
                .build();
    }

    public BaseResponseDto<PagedIncomeResponseDto> getAllIncomesByMonth(
            int page, int limit, YearMonth month
    ) {
        User user = this.userService.getAuthenticatedUser();

        Page<Income> incomes = this.incomeRepository.findAllByUserIdAndMonthOrderByTransactionDateTimeDesc(
                user.getId(), month, PageRequest.of(page - 1 , limit)
        );

        log.info("Successfully retrieved incomes for {} by user {}", month, user.getEmail());
        return BaseResponseDto.<PagedIncomeResponseDto>builder()
                .status("Success")
                .message("Successfully retrieved incomes for " + month)
                .data(new PagedIncomeResponseDto(incomes, this.getTotalIncomeByMonth(user.getId(), month)))
                .build();
    }

    public BaseResponseDto<PagedIncomeResponseDto> getAllIncomes(int page, int limit) {
        User user = this.userService.getAuthenticatedUser();

        Page<Income> incomes = this.incomeRepository
                .findAllByUserId(user.getId(), PageRequest.of(page - 1, limit));

        BigDecimal totalIncome = this.incomeRepository.sumIncome(user.getId());

        log.info("Successfully retrieved incomes for user {}", user.getEmail());
        return BaseResponseDto.<PagedIncomeResponseDto>builder()
                .status("Success")
                .message("Successfully retrieved all incomes")
                .data(new PagedIncomeResponseDto(incomes, totalIncome))
                .build();
    }

    @Transactional
    public BaseResponseDto<Object> deleteIncomeById(UUID id) {
        Income income = this.getIncomeById(id).getData();

        incomeRepository.deleteById(income.getId());

        log.info("Successfully deleted income with id {}", income.getId());
        return BaseResponseDto.builder()
                .status("Success")
                .message("Successfully deleted income with ID " + income.getId())
                .build();
    }

    @Transactional
    public BaseResponseDto<Object> deleteIncomesByIds(DeleteByIdsDto dto) {
        this.incomeRepository.deleteAllById(dto.getIds());

        log.info("Successfully deleted selected incomes");

        return BaseResponseDto.builder()
                .status("Success")
                .message("Successfully deleted selected incomes")
                .build();
    }

    public BaseResponseDto<List<MonthlyIncomeStatsResponseDto>> getMonthlyIncomeStats() {
        User user = this.userService.getAuthenticatedUser();

        List<Object[]> results = this.incomeRepository.sumIncomeByMonth(user.getId());

        List<MonthlyIncomeStatsResponseDto> stats = results.stream()
                .map(row -> new MonthlyIncomeStatsResponseDto(
                        (YearMonth) row[0],
                        (BigDecimal) row[1]
                ))
                .toList();

        log.info("Successfully retrieved monthly income stats");
        return BaseResponseDto.<List<MonthlyIncomeStatsResponseDto>>builder()
                .status("Success")
                .message("Successfully retrieved monthly income stats")
                .data(stats)
                .build();

    }

    public BigDecimal getTotalIncomeByMonth(UUID userId, YearMonth month) {
        return this.incomeRepository.getTotalIncomeByMonth(
                userId, month
        );
    }
}
