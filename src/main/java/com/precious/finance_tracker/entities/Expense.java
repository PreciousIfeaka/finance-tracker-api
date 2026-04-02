package com.precious.finance_tracker.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.precious.finance_tracker.enums.ExpenseCategory;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;

@SuperBuilder
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "expense", indexes = {
        @Index(name = "idx_expense_user_month_category", columnList = "user_id, month, category")
})
public class Expense extends AbstractBaseEntity {
    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private ExpenseCategory category;

    private String note;

    @Column(nullable = false)
    private Boolean isRecurring;

    @Column(nullable = false)
    private YearMonth month;

    private LocalDateTime transactionDateTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;
}
