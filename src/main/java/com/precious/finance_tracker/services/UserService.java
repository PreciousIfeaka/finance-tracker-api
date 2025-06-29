package com.precious.finance_tracker.services;

import com.precious.finance_tracker.dtos.BaseResponseDto;
import com.precious.finance_tracker.dtos.auth.RegisterUserDto;
import com.precious.finance_tracker.dtos.user.ChangePasswordDto;
import com.precious.finance_tracker.dtos.user.PagedUserResponseDto;
import com.precious.finance_tracker.dtos.user.UpdateUserRequestDto;
import com.precious.finance_tracker.dtos.user.UserResponseDto;
import com.precious.finance_tracker.entities.User;
import com.precious.finance_tracker.exceptions.BadRequestException;
import com.precious.finance_tracker.exceptions.NotFoundException;
import com.precious.finance_tracker.exceptions.UnauthorizedException;
import com.precious.finance_tracker.repositories.UserRepository;
import com.precious.finance_tracker.services.interfaces.IEmailService;
import com.precious.finance_tracker.services.interfaces.IUserService;
import jakarta.persistence.EntityManager;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Data
public class UserService implements IUserService {
    private static Logger log = LoggerFactory.getLogger(UserService.class.getName());

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final IEmailService emailService;
    private final EntityManager entityManager;

    @Transactional
    public User createUser(RegisterUserDto dto) {

        if (!dto.getConfirmPassword().equals(dto.getPassword())) {
            throw new BadRequestException("password and confirm password mismatch");
        }

        String otp = this.emailService.generateOtp();
        log.info("Otp {} set for user {}", otp, dto.getEmail());

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
            user = this.userRepository.findByEmailAndDeletedAtIsNull(email)
                    .orElseThrow(() -> new NotFoundException("User not found"));
        } else if (identifier instanceof UUID id) {
            user = this.userRepository.findByIdAndDeletedAtIsNull(id)
                    .orElseThrow(() -> new NotFoundException("User not found"));
        } else {
            throw new IllegalArgumentException(
                    "Unsupported identifier type: " + identifier.getClass()
            );
        }

        return UserResponseDto.fromEntity(user);
    }

    public PagedUserResponseDto getUsers(int page, int limit) {
        Page<User> users = this.userRepository.findByDeletedAtIsNull(PageRequest.of(page, limit));

        return new PagedUserResponseDto(users.map(UserResponseDto::fromEntity));
    }

    @Transactional
    public BaseResponseDto<UserResponseDto> updateUserDetails(UpdateUserRequestDto dto) {
        User user = this.getAuthenticatedUser();

        if (dto.getName() != null) user.setName(dto.getName());
        if (dto.getCurrency() != null) user.setCurrency(dto.getCurrency());
        if (dto.getAvatarUrl() != null) user.setAvatarUrl(dto.getAvatarUrl());

        User savedUser = this.userRepository.save(user);

        return BaseResponseDto.<UserResponseDto>builder()
                .status("Status")
                .message("Successfully updated user profile")
                .data(UserResponseDto.fromEntity(savedUser))
                .build();
    }

    @Transactional
    public BaseResponseDto<UserResponseDto> changePassword(ChangePasswordDto dto) {
        User user = this.getAuthenticatedUser();

        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new BadRequestException("Password and confirmPassword mismatch");
        } else if (passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BadRequestException("New password must not be same as old");
        }

        user.setPassword(passwordEncoder.encode(dto.getPassword()));

        this.userRepository.save(user);

        return BaseResponseDto.<UserResponseDto>builder()
                .status("Success")
                .message("Successfully updated password")
                .data(null)
                .build();
    }

    @Transactional
    public BaseResponseDto<Object> deleteUserData() {
        User user = this.getAuthenticatedUser();

        user.setDeletedAt(LocalDateTime.now());

        this.userRepository.save(user);

        return BaseResponseDto.builder()
                .status("Success")
                .message("Successfully deleted user account")
                .data(null)
                .build();
    }

    public User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated()) {
            String userEmail = auth.getName();

            return this.userRepository.findByEmailAndDeletedAtIsNull(userEmail)
                            .orElseThrow(() -> new UnauthorizedException("Unauthorized user"));
        }
        return null;
    }
}
