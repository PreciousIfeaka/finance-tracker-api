package com.precious.finance_tracker.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;


@SuperBuilder
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Income extends AbstractBaseEntity {
    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String source;

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
