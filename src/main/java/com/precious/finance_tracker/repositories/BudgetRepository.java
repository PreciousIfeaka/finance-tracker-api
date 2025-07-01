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
    Optional<Budget> findByIdAndDeletedAtIsNull(UUID uuid);

    Page<Budget> findAllByUserIdAndDeletedAtIsNull(UUID userId, Pageable pageable);

    @Query("""
        SELECT i FROM Budget i
        WHERE i.user.id = :userId
            AND i.amount = :amount
            AND i.category = :category
            AND i.deletedAt IS NULL
    """)
    Optional<Budget> findRecurringBudget(
            @Param("userId") UUID userId,
            @Param("amount") BigDecimal amount,
            @Param("category") ExpenseCategory category
    );

    @Query("""
        SELECT i FROM Budget i
        WHERE i.user.id = :userId
            AND i.month = :month
            AND i.deletedAt IS NULL
        ORDER BY i.createdAt DESC
    """)
    Page<Budget> findByUserAndDate(
            @Param("userId") UUID userId,
            @Param("month") YearMonth month,
            Pageable pageable
    );

    @Query("""
        SELECT COALESCE(SUM(i.amount), 0)
        FROM Budget i
        WHERE i.user.id = :userId
          AND i.deletedAt IS NULL
          AND i.month = :month
    """)
    BigDecimal getTotalBudgetByMonth(@Param("userId") UUID userId, @Param("month") YearMonth month);

    @Query("""
        SELECT i FROM Budget i
        WHERE i.user.id = :userId
            AND i.month = :month
            AND i.category = :category
            AND i.deletedAt IS NULL
    """)
    Optional<Budget> findByUserAndCategoryAndDate(
            @Param("userId") UUID userId,
            @Param("month") YearMonth month,
            @Param("category") ExpenseCategory category
    );

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
}
