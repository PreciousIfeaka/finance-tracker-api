package com.precious.finance_tracker.repositories;

import com.precious.finance_tracker.entities.Expense;
import com.precious.finance_tracker.enums.ExpenseCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
    Optional<Expense> findByIdAndDeletedAtIsNull(UUID id);

    Page<Expense> findByUserIdAndDeletedAtIsNull(UUID userId, Pageable pageable);

    @Query("""
        SELECT i FROM Expense i
        WHERE i.user.id = :userId
            AND i.dueDate = :dueDate
            AND i.amount = :amount
            AND i.category = :category
            AND i.deletedAt IS NULL
    """)
    Optional<Expense> findRecurringExpense(
            @Param("userId") UUID userId,
            @Param("dueDate") LocalDate dueDate,
            @Param("amount") BigDecimal amount,
            @Param("category") ExpenseCategory category
            );

    @Query("""
        SELECT i FROM Expense i
        WHERE i.user.id = :userId
            AND EXTRACT(YEAR FROM i.dueDate) = :year
            AND EXTRACT(MONTH FROM i.dueDate) = :month
            AND i.deletedAt IS NULL
        ORDER BY i.dueDate DESC
    """)
    Page<Expense> findByUserAndDate(
            @Param("userId") UUID userId,
            @Param("year") int year,
            @Param("month") int month,
            Pageable pageable
    );

    @Query(value = """
        SELECT TO_CHAR(i.dueDate, 'YYYY-MM') AS month,
               SUM(i.amount) AS total
        FROM Expense i
        WHERE i.user.id = :userId AND i.deletedAt IS NULL
        GROUP BY TO_CHAR(i.dueDate, 'YYYY-MM')
        ORDER BY month
    """)
    List<Object[]> sumExpenseByMonth(@Param("userId") UUID userId);
}
