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
    Page<Expense> findAllByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<Expense> findAllByUserIdAndMonthOrderByTransactionDateTimeDesc(UUID userId, YearMonth month, Pageable pageable);
    List<Expense> findAllByUserIdAndMonthOrderByTransactionDateTimeDesc(UUID userId, YearMonth month);

    @Query("""
        SELECT COALESCE(SUM(e.amount), 0)
        FROM Expense e
        WHERE e.user.id = :userId
          AND e.deletedAt IS NULL
          AND (:month IS NULL OR e.month = :month)
    """)
    BigDecimal getTotalExpenseByMonth(@Param("userId") UUID userId, @Param("month") YearMonth month);

    @Query(value = """
        SELECT e.month AS month,
               COALESCE(SUM(e.amount), 0) AS total
        FROM Expense e
        WHERE e.user.id = :userId AND e.deletedAt IS NULL
        GROUP BY e.month
        ORDER BY e.month
    """)
    List<Object[]> sumExpenseByMonth(@Param("userId") UUID userId);

    @Query(value = """
        SELECT COALESCE(SUM(e.amount), 0)
        FROM Expense e
        WHERE e.user.id = :userId
            AND e.deletedAt IS NULL
    """)
    BigDecimal sumExpense(@Param("userId") UUID userId);

    Page<Expense> findAllByUserIdAndMonthAndCategoryOrderByTransactionDateTimeDesc(
            UUID userId,
            YearMonth month,
            ExpenseCategory category,
            Pageable pageable
    );

    @Query(value = """
        SELECT COALESCE(SUM(e.amount), 0)
        FROM Expense e
        WHERE e.user.id = :userId
            AND e.month = :month
            AND e.category = :category
            AND e.deletedAt IS NULL
    """)
    BigDecimal sumExpenseByUserIdAndMonthAndCategory(
            @Param("userId") UUID userId,
            @Param("month") YearMonth month,
            @Param("category") ExpenseCategory category
    );

    Optional<Expense> findByIdAndUserId(UUID id, UUID userId);
}
