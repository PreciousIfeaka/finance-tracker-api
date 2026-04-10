package com.precious.finance_tracker.repositories;

import com.precious.finance_tracker.entities.Budget;
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

public interface BudgetRepository extends JpaRepository<Budget, UUID> {
    Optional<Budget> findByIdAndUserId(UUID id, UUID userId);

    Page<Budget> findAllByUserId(UUID userId, Pageable pageable);

    Optional<Budget> findByUserIdAndAmountAndCategory(UUID userId, BigDecimal amount, ExpenseCategory category);

    Page<Budget> findAllByUserIdAndMonthOrderByCreatedAtDesc(UUID userId, YearMonth month, Pageable pageable);

    @Query("""
        SELECT COALESCE(SUM(i.amount), 0)
        FROM Budget i
        WHERE i.user.id = :userId
          AND i.deletedAt IS NULL
          AND i.month = :month
    """)
    BigDecimal getTotalBudgetByMonth(@Param("userId") UUID userId, @Param("month") YearMonth month);

    Optional<Budget> findByUserIdAndMonthAndCategory(UUID userId, YearMonth month, ExpenseCategory category);

    @Query(value = """
        SELECT i.month AS month,
               COALESCE(SUM(i.amount), 0) AS total
        FROM Budget i
        WHERE i.user.id = :userId AND i.deletedAt IS NULL
        GROUP BY i.month
        ORDER BY i.month
    """)
    List<Object[]> sumBudgetByMonth(@Param("userId") UUID userId);

    @Query(value = """
        SELECT COALESCE(SUM(i.amount), 0)
        FROM Budget i
        WHERE i.user.id = :userId
            AND i.deletedAt IS NULL
    """)
    BigDecimal sumBudget(@Param("userId") UUID userId);

    @Query("""
        SELECT b.category AS category,
               COALESCE(SUM(b.amount), 0) AS total,
               MAX(CASE WHEN b.isExceeded = true THEN 1 ELSE 0 END) AS exceeded
        FROM Budget b
        WHERE b.user.id = :userId
          AND b.deletedAt IS NULL
          AND b.month = :month
        GROUP BY b.category
        ORDER BY total DESC
    """)
    List<Object[]> sumBudgetGroupedByCategoryAndMonth(
            @Param("userId") UUID userId,
            @Param("month") YearMonth month
    );

    @Query("""
        SELECT b.category AS category,
               COALESCE(SUM(b.amount), 0) AS total,
               MAX(CASE WHEN b.isExceeded = true THEN 1 ELSE 0 END) AS exceeded
        FROM Budget b
        WHERE b.user.id = :userId
          AND b.deletedAt IS NULL
          AND SUBSTRING(b.month, 1, 4) = :year
        GROUP BY b.category
        ORDER BY total DESC
    """)
    List<Object[]> sumBudgetGroupedByCategoryAndYear(
            @Param("userId") UUID userId,
            @Param("year") int year
    );
}
