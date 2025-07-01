package com.precious.finance_tracker.repositories;

import com.precious.finance_tracker.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByIdAndDeletedAtIsNull(UUID uuid);

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    Optional<User> findByOtpAndDeletedAtIsNull(String otp);

    Page<User> findByDeletedAtIsNull(Pageable pageable);
}
