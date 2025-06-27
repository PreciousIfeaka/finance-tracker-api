package com.precious.finance_tracker.repositories;

import com.precious.finance_tracker.entities.Income;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IncomeRepository extends JpaRepository<Income, UUID> {
    List<Income> findByUserId(UUID userId);
}
