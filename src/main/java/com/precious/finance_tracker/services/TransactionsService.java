package com.precious.finance_tracker.services;

import com.precious.finance_tracker.dtos.BaseResponseDto;
import com.precious.finance_tracker.dtos.transactions.*;
import com.precious.finance_tracker.entities.Transactions;
import com.precious.finance_tracker.entities.User;
import com.precious.finance_tracker.enums.TransactionDirection;
import com.precious.finance_tracker.exceptions.BadRequestException;
import com.precious.finance_tracker.exceptions.ConflictResourceException;
import com.precious.finance_tracker.exceptions.NotFoundException;
import com.precious.finance_tracker.repositories.TransactionRepository;
import com.precious.finance_tracker.services.interfaces.ITransactionService;
import lombok.Data;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Data
public class TransactionsService implements ITransactionService {
    private final TransactionRepository transactionRepository;
    private final UserService userService;

    public void createTransaction(CreateTransactionDto dto) {
        User user = this.userService.getAuthenticatedUser();

        Optional<Transactions> existingTransaction = this.transactionRepository.findByUserAndAmountAndDirectionAndDeletedAtIsNull(
                user.getId(), dto.getDirection(), dto.getAmount()
        );

        if (user.getCurrency() == null) {
            throw new BadRequestException("Currency has not been set in user profile");
        } else if (
                existingTransaction.isPresent() &&
                        LocalDateTime.now().getMinute() - existingTransaction.get().getCreatedAt().getMinute() < 2
        ) {
            throw new ConflictResourceException("Duplicate transaction entry, try again after 2 mins.");
        }

        Transactions transaction = Transactions.builder()
                .amount(dto.getAmount())
                .direction(dto.getDirection())
                .month(YearMonth.now())
                .user(user)
                .description(dto.getDescription())
                .build();

        this.transactionRepository.save(transaction);
    }

    public BaseResponseDto<Transactions> updateTransaction(UUID id, UpdateTransactionDto dto) {
        User user = this.userService.getAuthenticatedUser();

        Transactions transaction = this.transactionRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException("Transaction record not found"));

        if (dto.getAmount() != null) transaction.setAmount(dto.getAmount());
        if (dto.getDescription() != null) transaction.setDescription(dto.getDescription());
        if (dto.getDirection() != null) transaction.setDirection(dto.getDirection());

        return BaseResponseDto.<Transactions>builder()
                .status("Success")
                .message("Successfully updated transaction record")
                .data(this.transactionRepository.save(transaction))
                .build();
    }

    public BaseResponseDto<Transactions> getTransactionById(UUID id) {
        Transactions transaction = this.transactionRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException("Transaction record not found"));

        return BaseResponseDto.<Transactions>builder()
                .status("Success")
                .message("Successfully retrieved transaction record")
                .data(transaction)
                .build();
    }

    public BaseResponseDto<PagedTransactionResponseDto> getFilteredTransactions(
        int page, int limit, YearMonth month, TransactionDirection direction
    ) {
        User user = this.userService.getAuthenticatedUser();

        Page<Transactions> transactions = this.transactionRepository.findByUserAndDateAndDirection(
                user.getId(),
                month,
                direction,
                PageRequest.of(page - 1, limit)
        );

        return BaseResponseDto.<PagedTransactionResponseDto>builder()
                .status("Success")
                .message("Successfully retrieved transactions")
                .data(
                        new PagedTransactionResponseDto(
                                transactions,
                                this.getTransactionSum(user.getId(), month, direction)
                        )
                )
                .build();
    }

    public BaseResponseDto<List<MonthlyTransactionStatsResponseDto>> getMonthlyTransactionsStats(
            TransactionDirection direction
    ) {
        User user = this.getUserService().getAuthenticatedUser();

        List<Object[]> results = this.transactionRepository.sumTransactionsByMonth(
                user.getId(), direction
        );

        List<MonthlyTransactionStatsResponseDto> stats = results.stream().map(
                row -> new MonthlyTransactionStatsResponseDto(
                        (YearMonth) row[0],
                        (BigDecimal) row[1]
                )
        ).toList();

        return BaseResponseDto.<List<MonthlyTransactionStatsResponseDto>>builder()
                .status("Success")
                .message("Successfully retrieved monthly transaction stats")
                .data(stats)
                .build();
    }

    public BaseResponseDto<Object> deleteTransactionById(UUID id) {
        User user = this.userService.getAuthenticatedUser();

        Transactions transaction = this.transactionRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException("Transaction record not found"));

        transaction.setDeletedAt(LocalDateTime.now());

        this.transactionRepository.save(transaction);

        return BaseResponseDto.builder()
                .status("Success")
                .message("Successfully deleted transaction record")
                .data(null)
                .build();
    }

    private BigDecimal getTransactionSum(UUID userId, YearMonth month, TransactionDirection direction) {

        TransactionTotals totals = this.transactionRepository.getTotalUserTransactionsAmount(userId, null, month);

        BigDecimal sum;

        if (direction == null) {
            sum = totals.getCredit().subtract(totals.getDebit());
        } else {
            sum = direction == TransactionDirection.debit
                    ? totals.getDebit()
                    : totals.getCredit();
        }

        System.out.println(sum);
        return sum;
    }
}
