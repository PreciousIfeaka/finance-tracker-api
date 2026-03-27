package com.precious.finance_tracker.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.precious.finance_tracker.enums.StatementAnalysisStatus;
import com.precious.finance_tracker.helpers.YearMonthConverter;
import com.precious.finance_tracker.types.DocumentUrls;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.YearMonth;
import java.util.List;

@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
public class BankStatement extends  AbstractBaseEntity {
    @ElementCollection
    private List<DocumentUrls> documentUrls;

    @Column(nullable = false)
    @Convert(converter = YearMonthConverter.class)
    private YearMonth month;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private StatementAnalysisStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;
}

