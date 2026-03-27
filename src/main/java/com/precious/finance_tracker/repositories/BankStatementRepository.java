package com.precious.finance_tracker.repositories;

import com.precious.finance_tracker.entities.BankStatement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

public interface BankStatementRepository extends JpaRepository<BankStatement, UUID> {
    Optional<BankStatement> findByIdAndDeletedAtIsNull(UUID id);
    Optional<BankStatement> findByIdAndUserIdAndDeletedAtIsNull(UUID uuid, UUID userId);
    Optional<BankStatement> findByUserIdAndMonthAndDeletedAtIsNull(UUID userId, YearMonth month);
    Page<BankStatement> findAllByUserIdAndDeletedAtIsNull(UUID userId, Pageable pageable);
}
