package com.precious.finance_tracker.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.precious.finance_tracker.enums.IncomeType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;

import java.math.BigDecimal;
import java.time.LocalDate;


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

    @Enumerated(EnumType.STRING)
    private IncomeType type;

    private String note;

    @Column(nullable = false)
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;
}
