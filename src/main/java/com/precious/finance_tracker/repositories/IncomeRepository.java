package com.precious.finance_tracker.repositories;

import com.precious.finance_tracker.entities.Income;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IncomeRepository extends JpaRepository<Income, UUID> {
    Page<Income> findByUserIdAndDeletedAtIsNull(UUID userId, Pageable pageable);

    @Query("""
        SELECT i FROM Income i
        WHERE i.user.id = :userId
            AND i.date = :date
            AND i.amount = :amount
            AND i.source = :source
            AND i.deletedAt IS NULL
    """)
    Optional<Income> findRecurringIncome(
            @Param("userId") UUID userId,
            @Param("date") LocalDate date,
            @Param("amount")BigDecimal amount,
            @Param("source") String source
    );

    @Query("""
        SELECT i FROM Income i
        WHERE i.user.id = :userId
            AND EXTRACT(YEAR FROM i.date) = :year
            AND EXTRACT(MONTH FROM i.date) = :month
            AND i.deletedAt IS NULL
        ORDER BY i.date DESC
    """)
    Page<Income> findByUserAndDate(
            @Param("userId") UUID userId,
            @Param("year") int year,
            @Param("month") int month,
            Pageable pageable
    );

    @Query(value = """
        SELECT TO_CHAR(i.date, 'YYYY-MM') AS month,
               SUM(i.amount) AS total
        FROM Income i
        WHERE i.user.id = :userId AND i.deletedAt IS NULL
        GROUP BY TO_CHAR(i.date, 'YYYY-MM')
        ORDER BY month
    """)
    List<Object[]> sumIncomeByMonth(@Param("userId") UUID userId);

    Optional<Income> findByIdAndDeletedAtIsNull(UUID id);
}
