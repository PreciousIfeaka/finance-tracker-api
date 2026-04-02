package com.precious.finance_tracker.repositories;

import com.precious.finance_tracker.dtos.transactions.TransactionTotals;
import com.precious.finance_tracker.entities.Transactions;
import com.precious.finance_tracker.enums.TransactionDirection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transactions, UUID> {
    Page<Transactions> findByUserIdAndMonthAndDirection(
            @Param("userId") UUID userId,
            @Param("month") YearMonth month,
            @Param("direction") TransactionDirection direction,
            Pageable pageable
    );


    @Query(value = """
        SELECT t.month AS month,
               COALESCE(SUM(t.amount), 0) AS total
        FROM Transactions t
        WHERE t.user.id = :userId
            AND t.deletedAt IS NULL
            AND (:direction IS NULL OR t.direction = :direction)
        GROUP BY t.month
        ORDER BY t.month
    """)
    List<Object[]> sumTransactionsByMonth(
            @Param("userId") UUID userId,
            @Param("direction") TransactionDirection direction
    );

    @Query("""
        SELECT
            COALESCE(SUM(CASE WHEN t.direction = 'credit' THEN t.amount END), 0) AS credit,
            COALESCE(SUM(CASE WHEN t.direction = 'debit' THEN t.amount END), 0) AS debit
        FROM Transactions t
        WHERE t.user.id = :userId
          AND t.deletedAt IS NULL
          AND (:month IS NULL OR t.month = :month)
    """)
    TransactionTotals getTotalUserTransactionsAmount(
            @Param("userId") UUID userId,
            @Param("direction") TransactionDirection direction,
            @Param("month") YearMonth month
    );

    Optional<Transactions> findByIdAndUserId(UUID id, UUID userId);

    Optional<Transactions> findByUserIdAndAmountAndDirectionAndTransactionDateTime(
            UUID userId, BigDecimal amount, TransactionDirection direction, LocalDateTime transactionDateTime
    );
}
