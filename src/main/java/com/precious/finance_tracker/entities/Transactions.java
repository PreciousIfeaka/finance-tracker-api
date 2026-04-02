package com.precious.finance_tracker.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.precious.finance_tracker.enums.TransactionDirection;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;

@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_trx_user_month_direction", columnList = "user_id, month, direction")
})
public class Transactions extends AbstractBaseEntity {
    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private YearMonth month;

    @Column
    @Enumerated(value = EnumType.STRING)
    private TransactionDirection direction;

    @Column
    private String description;

    private LocalDateTime transactionDateTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;
}
