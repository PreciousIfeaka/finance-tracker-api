package com.precious.finance_tracker.services;

import com.precious.finance_tracker.dtos.BaseResponseDto;
import com.precious.finance_tracker.dtos.auth.RegisterUserDto;
import com.precious.finance_tracker.dtos.user.ChangePasswordDto;
import com.precious.finance_tracker.dtos.user.PagedUserResponseDto;
import com.precious.finance_tracker.dtos.user.UpdateUserRequestDto;
import com.precious.finance_tracker.dtos.user.UserResponseDto;
import com.precious.finance_tracker.entities.User;
import com.precious.finance_tracker.enums.Currency;
import com.precious.finance_tracker.exceptions.BadRequestException;
import com.precious.finance_tracker.exceptions.NotFoundException;
import com.precious.finance_tracker.exceptions.UnauthorizedException;
import com.precious.finance_tracker.repositories.UserRepository;
import com.precious.finance_tracker.services.interfaces.IEmailService;
import com.precious.finance_tracker.services.interfaces.IS3UploadService;
import com.precious.finance_tracker.services.interfaces.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements IUserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final IEmailService emailService;
    private final IS3UploadService s3UploadService;

    @Transactional
    public User createUser(RegisterUserDto dto) {
        Optional<User> existingUser = this.userRepository.findByEmail(dto.getEmail());

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            if (user.getIsVerified()) {
                return user;
            }
            user.setOtp(this.emailService.generateOtp());
            return this.userRepository.save(user);
        }

        if (!dto.getConfirmPassword().equals(dto.getPassword())) {
            throw new BadRequestException("password and confirm password mismatch");
        }

        String otp = this.emailService.generateOtp();

        User user = User.builder()
                .name(dto.getFirstName() + " " + dto.getLastName())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .otp(otp)
                .currency(Currency.NGN)
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
            throw new IllegalArgumentException(
                    "Unsupported identifier type: " + identifier.getClass());
        }

        return UserResponseDto.fromEntity(user, s3UploadService);
    }

    public PagedUserResponseDto getUsers(int page, int limit) {
        Page<User> users = this.userRepository.findAll(PageRequest.of(page, limit));

        return new PagedUserResponseDto(users.map(user -> UserResponseDto.fromEntity(user, s3UploadService)));
    }

    @Transactional
    public BaseResponseDto<UserResponseDto> updateUserDetails(UpdateUserRequestDto dto) {
        User user = this.getAuthenticatedUser();

        if (dto.name() != null)
            user.setName(dto.name());
        if (dto.currency() != null)
            user.setCurrency(dto.currency());
        if (dto.avatarUrl() != null) {
            if (user.getAvatarUrl() != null) {
                this.s3UploadService.deleteFromS3(user.getAvatarUrl());
            }
            user.setAvatarUrl(dto.avatarUrl());
        }

        User savedUser = this.userRepository.save(user);

        log.info("Successfully updated user info for {}", user.getEmail());
        return BaseResponseDto.<UserResponseDto>builder()
                .status("Success")
                .message("Successfully updated user profile")
                .data(UserResponseDto.fromEntity(savedUser, s3UploadService))
                .build();
    }

    @Transactional
    public BaseResponseDto<UserResponseDto> changePassword(ChangePasswordDto dto) {
        User user = this.getAuthenticatedUser();

        if (!dto.password().equals(dto.confirmPassword())) {
            throw new BadRequestException("Password and confirmPassword mismatch");
        } else if (passwordEncoder.matches(dto.password(), user.getPassword())) {
            throw new BadRequestException("New password must not be same as old");
        }

        user.setPassword(passwordEncoder.encode(dto.password()));

        this.userRepository.save(user);

        log.info("Successfully changed password for user {}", user.getEmail());
        return BaseResponseDto.<UserResponseDto>builder()
                .status("Success")
                .message("Successfully updated password")
                .data(null)
                .build();
    }

    @Transactional
    public BaseResponseDto<Object> deleteUserData() {
        User user = this.getAuthenticatedUser();

        this.userRepository.deleteById(user.getId());

        log.info("Successfully deleted user record for {}", user.getEmail());
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

            return this.userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new UnauthorizedException("Unauthorized user"));
        }
        return null;
    }
}
