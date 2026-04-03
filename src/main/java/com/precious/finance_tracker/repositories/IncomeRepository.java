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
    Page<Income> findAllByUserId(UUID userId, Pageable pageable);

    Page<Income> findAllByUserIdAndMonthOrderByTransactionDateTimeDesc(UUID userId, YearMonth month, Pageable pageable);

    List<Income> findAllByUserIdAndMonthOrderByTransactionDateTimeDesc(UUID userId, YearMonth month);

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
          AND (:month IS NULL OR i.month = :month)
    """)
    BigDecimal getTotalIncomeByMonth(@Param("userId") UUID userId, @Param("month") YearMonth month);

    Optional<Income> findByIdAndUserId(UUID id, UUID userId);
}
