package com.precious.finance_tracker.services.interfaces;

import com.precious.finance_tracker.dtos.BaseResponseDto;
import com.precious.finance_tracker.dtos.transactions.CreateTransactionDto;
import com.precious.finance_tracker.dtos.transactions.MonthlyTransactionStatsResponseDto;
import com.precious.finance_tracker.dtos.transactions.PagedTransactionResponseDto;
import com.precious.finance_tracker.dtos.transactions.UpdateTransactionDto;
import com.precious.finance_tracker.entities.Transactions;
import com.precious.finance_tracker.enums.TransactionDirection;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

public interface ITransactionService {
    void createTransaction(CreateTransactionDto dto);

    BaseResponseDto<Transactions> updateTransaction(UUID id, UpdateTransactionDto dto);

    BaseResponseDto<Transactions> getTransactionById(UUID id);

    BaseResponseDto<PagedTransactionResponseDto> getFilteredTransactions(
            int page, int limit, YearMonth month, TransactionDirection direction
    );
    BaseResponseDto<Object> deleteTransactionById(UUID id);

    BaseResponseDto<List<MonthlyTransactionStatsResponseDto>> getMonthlyTransactionsStats(
            TransactionDirection direction
    );
}
