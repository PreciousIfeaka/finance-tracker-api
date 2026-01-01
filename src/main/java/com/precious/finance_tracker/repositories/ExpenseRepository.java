package com.precious.finance_tracker.repositories;

import com.precious.finance_tracker.entities.Expense;
import com.precious.finance_tracker.enums.ExpenseCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
    Optional<Expense> findByIdAndDeletedAtIsNull(UUID id);

    Page<Expense> findByUserIdAndDeletedAtIsNull(UUID userId, Pageable pageable);

    @Query("""
        SELECT i FROM Expense i
        WHERE i.user.id = :userId
            AND i.amount = :amount
            AND i.category = :category
            AND i.deletedAt IS NULL
    """)
    Optional<Expense> findRecurringExpense(
            @Param("userId") UUID userId,
            @Param("amount") BigDecimal amount,
            @Param("category") ExpenseCategory category
            );

    @Query("""
        SELECT i FROM Expense i
        WHERE i.user.id = :userId
            AND i.month = month
            AND i.deletedAt IS NULL
        ORDER BY i.createdAt DESC
    """)
    Page<Expense> findByUserAndDate(
            @Param("userId") UUID userId,
            @Param("month") YearMonth month,
            Pageable pageable
    );

    @Query("""
        SELECT COALESCE(SUM(i.amount), 0)
        FROM Expense i
        WHERE i.user.id = :userId
          AND i.deletedAt IS NULL
          AND (:month IS NULL OR i.month = :month)
    """)
    BigDecimal getTotalExpenseByMonth(@Param("userId") UUID userId, @Param("month") YearMonth month);

    @Query(value = """
        SELECT i.month AS month,
               COALESCE(SUM(i.amount), 0) AS total
        FROM Expense i
        WHERE i.user.id = :userId AND i.deletedAt IS NULL
        GROUP BY i.month
        ORDER BY i.month
    """)
    List<Object[]> sumExpenseByMonth(@Param("userId") UUID userId);

    @Query(value = """
        SELECT COALESCE(SUM(i.amount), 0)
        FROM Expense i
        WHERE i.user.id = :userId
            AND i.deletedAt IS NULL
    """)
    BigDecimal sumExpense(@Param("userId") UUID userId);

    @Query("""
        SELECT i FROM Expense i
        WHERE i.user.id = :userId
            AND i.month = :month
            AND i.category = :category
            AND i.deletedAt IS NULL
    """)
    Optional<Expense> findByUserAndCategoryAndDate(
            @Param("userId") UUID userId,
            @Param("month") YearMonth month,
            @Param("category") ExpenseCategory category
    );
}
