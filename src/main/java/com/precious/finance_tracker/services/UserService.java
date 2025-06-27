package com.precious.finance_tracker.services;

import com.precious.finance_tracker.dtos.auth.RegisterUserDto;
import com.precious.finance_tracker.dtos.user.UserResponseDto;
import com.precious.finance_tracker.entities.User;
import com.precious.finance_tracker.exceptions.BadRequestException;
import com.precious.finance_tracker.exceptions.NotFoundException;
import com.precious.finance_tracker.exceptions.UnauthorizedException;
import com.precious.finance_tracker.repositories.UserRepository;
import lombok.Data;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Data
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public User createUser(RegisterUserDto dto) {
        if (!dto.getConfirmPassword().equals(dto.getPassword())) {
            throw new BadRequestException("password and confirm password mismatch");
        }

        String otp = this.emailService.generateOtp();

        User user = User.builder()
                .name(dto.getFirstName() + " " + dto.getLastName())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .otp(otp)
                .otpExpiredAt(LocalDateTime.now().plusMinutes(10))
                .isVerified(false)
                .build();

        return this.userRepository.save(user);
    }

    public UserResponseDto getUser(Object identifier) {
        User user;

        if (identifier instanceof String email && email.contains("@")) {
            user = this.userRepository.findByEmail(email)
                    .orElseThrow(() -> new NotFoundException("User not found"));
        } else if (identifier instanceof UUID id) {
            user = this.userRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("User not found"));
        } else {
            throw new IllegalArgumentException("Unsupported identifier type: " + identifier.getClass());
        }

        return UserResponseDto.fromEntity(user);
    }

    protected User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated()) {
            String userEmail = auth.getName();

            return this.userRepository.findByEmail(userEmail)
                            .orElseThrow(() -> new UnauthorizedException("Unauthorized user"));
        }

        return null;
    }
}
