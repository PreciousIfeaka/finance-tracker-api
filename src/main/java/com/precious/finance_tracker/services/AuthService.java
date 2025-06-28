package com.precious.finance_tracker.services;

import com.precious.finance_tracker.configurations.JwtService;
import com.precious.finance_tracker.dtos.BaseResponseDto;
import com.precious.finance_tracker.dtos.auth.*;
import com.precious.finance_tracker.dtos.email.VerifyEmailDto;
import com.precious.finance_tracker.dtos.user.UserResponseDto;
import com.precious.finance_tracker.entities.User;
import com.precious.finance_tracker.enums.EmailPurpose;
import com.precious.finance_tracker.exceptions.BadRequestException;
import com.precious.finance_tracker.exceptions.ForbiddenException;
import com.precious.finance_tracker.exceptions.InternalServerError;
import com.precious.finance_tracker.exceptions.NotFoundException;
import com.precious.finance_tracker.repositories.UserRepository;
import jakarta.mail.MessagingException;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Data
@Transactional
public class AuthService {
    private static Logger log = LoggerFactory.getLogger(AuthService.class.getName());

    private final UserService userService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public BaseResponseDto<UserResponseDto> registerUser(RegisterUserDto dto) {
        User createdUser = this.userService.createUser(dto);

        this.emailService.sendOtpEmailAsync(
                VerifyEmailDto.builder()
                        .firstName(dto.getFirstName())
                        .recipientEmail(dto.getEmail())
                        .otp(createdUser.getOtp())
                        .purpose(EmailPurpose.email_verification)
                        .build()
        );

        return BaseResponseDto.<UserResponseDto>builder()
                .status("Success")
                .message("Successfully registered user, check your email for otp")
                .data(UserResponseDto.fromEntity(createdUser))
                .build();
    }

    public BaseResponseDto<AuthResponseDto> login(LoginUserDto dto) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            dto.getEmail().toLowerCase(),
                            dto.getPassword()
                    )
            );
        } catch (AuthenticationException e) {
            throw new BadRequestException("Invalid email or password");
        }

        UserResponseDto user = this.userService.getUser(dto.getEmail());

        if (!user.isVerified()) throw new ForbiddenException("User is not verified");

        String accessToken = this.jwtService.generateToken(user.getEmail());

        return BaseResponseDto.<AuthResponseDto>builder()
                .status("Success")
                .message("Successfully signed in")
                .data(AuthResponseDto.builder().user(user).accessToken(accessToken).build())
                .build();
    }

    public BaseResponseDto<AuthResponseDto> verifyOtp(VerifyOtpDto dto) {
        User user = this.userRepository.findByEmailAndDeletedAtIsNull(dto.getEmail())
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!user.getOtp().equals(dto.getOtp()) || user.getOtpExpiredAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Invalid OTP");
        }

        user.setOtp(null);
        user.setIsVerified(true);

        this.userRepository.save(user);

        AuthResponseDto authResponse = AuthResponseDto.builder()
                .user(UserResponseDto.fromEntity(user))
                .accessToken(this.jwtService.generateToken(user.getEmail()))
                .build();

        return BaseResponseDto.<AuthResponseDto>builder()
                .status("Success")
                .message("Successful OTP verification")
                .data(authResponse)
                .build();
    }

    public BaseResponseDto<Object> resendOtp(String email, EmailPurpose purpose) {
        User user = this.userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new NotFoundException("User not found"));

        user.setOtp(this.emailService.generateOtp());
        user.setOtpExpiredAt(LocalDateTime.now().plusMinutes(10));
        user.setIsVerified(false);

        this.userRepository.save(user);

        if (purpose == EmailPurpose.resend_otp) {
            this.emailService.sendOtpEmailAsync(
                    VerifyEmailDto.builder()
                            .firstName(user.getName().split(" ")[0])
                            .recipientEmail(user.getEmail())
                            .otp(user.getOtp())
                            .purpose(EmailPurpose.resend_otp)
                            .build()
            );
        } else {
            this.emailService.sendOtpEmailAsync(
                    VerifyEmailDto.builder()
                            .firstName(user.getName().split(" ")[0])
                            .recipientEmail(user.getEmail())
                            .otp(user.getOtp())
                            .purpose(EmailPurpose.forgot_password)
                            .build()
            );
        }

        return BaseResponseDto.builder()
                .status("Success")
                .message("OTP has been successfully sent")
                .data(null)
                .build();
    }

    public BaseResponseDto<Object> resetPassword(ResetPasswordDto dto) {
        User user = this.userRepository.findByOtpAndDeletedAtIsNull(dto.getOtp())
                .orElseThrow(() -> new BadRequestException("Invalid OTP"));

        if (user.getOtpExpiredAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Invalid OTP");
        } else if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new BadRequestException("Password mismatch");
        }

        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setIsVerified(true);

        this.userRepository.save(user);

        return BaseResponseDto.builder()
                .status("Success")
                .message("Successfully reset password")
                .data(null)
                .build();
    }
}
