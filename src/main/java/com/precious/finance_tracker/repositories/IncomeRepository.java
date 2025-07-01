package com.precious.finance_tracker.repositories;

import com.precious.finance_tracker.entities.Income;
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

public interface IncomeRepository extends JpaRepository<Income, UUID> {
    Page<Income> findByUserIdAndDeletedAtIsNull(UUID userId, Pageable pageable);

    @Query("""
        SELECT i FROM Income i
        WHERE i.user.id = :userId
            AND i.amount = :amount
            AND i.source = :source
            AND i.deletedAt IS NULL
    """)
    Optional<Income> findRecurringIncome(
            @Param("userId") UUID userId,
            @Param("amount") BigDecimal amount,
            @Param("source") String source
    );

    @Query("""
        SELECT i FROM Income i
        WHERE i.user.id = :userId
            AND i.month = :month
            AND i.deletedAt IS NULL
        ORDER BY i.createdAt DESC
    """)
    Page<Income> findByUserAndDate(
            @Param("userId") UUID userId,
            @Param("month") YearMonth month,
            Pageable pageable
    );

    @Query("""
        SELECT i FROM Income i
        WHERE i.user.id = :userId
            AND i.month = :month
            AND i.deletedAt IS NULL
        ORDER BY i.createdAt DESC
    """)
    List<Income> findByUserAndDate(
            @Param("userId") UUID userId,
            @Param("month") YearMonth month
    );

    @Query(value = """
        SELECT i.month AS month,
               COALESCE(SUM(i.amount), 0) AS total
        FROM Income i
        WHERE i.user.id = :userId AND i.deletedAt IS NULL
        GROUP BY i.month
        ORDER BY i.month
    """)
    List<Object[]> sumIncomeByMonth(@Param("userId") UUID userId);

    @Query(value = """
        SELECT COALESCE(SUM(i.amount), 0)
        FROM Income i
        WHERE i.user.id = :userId
            AND i.deletedAt IS NULL
    """)
    BigDecimal sumIncome(@Param("userId") UUID userId);

    @Query("""
        SELECT COALESCE(SUM(i.amount), 0)
        FROM Income i
        WHERE i.user.id = :userId
          AND i.deletedAt IS NULL
          AND i.month = :month
    """)
    BigDecimal getTotalIncomeByMonth(@Param("userId") UUID userId, @Param("month") YearMonth month);

    Optional<Income> findByIdAndDeletedAtIsNull(UUID id);
}
