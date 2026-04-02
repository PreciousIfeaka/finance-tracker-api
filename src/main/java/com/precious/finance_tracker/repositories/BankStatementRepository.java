package com.precious.finance_tracker.repositories;

import com.precious.finance_tracker.entities.BankStatement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

public interface BankStatementRepository extends JpaRepository<BankStatement, UUID> {
    Optional<BankStatement> findByIdAndUserId(UUID id, UUID userId);
    Optional<BankStatement> findByUserIdAndMonth(UUID userId, YearMonth month);
    Page<BankStatement> findAllByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
