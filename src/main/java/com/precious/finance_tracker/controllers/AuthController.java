package com.precious.finance_tracker.controllers;

import com.precious.finance_tracker.dtos.BaseResponseDto;
import com.precious.finance_tracker.dtos.auth.*;
import com.precious.finance_tracker.dtos.user.UserResponseDto;
import com.precious.finance_tracker.enums.EmailPurpose;
import com.precious.finance_tracker.services.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Data
@Tag(name = "Auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<BaseResponseDto<UserResponseDto>> registerUser(
          @Valid  @RequestBody RegisterUserDto dto
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(this.authService.registerUser(dto));
    }

    @PostMapping("/login")
    public ResponseEntity<BaseResponseDto<AuthResponseDto>> loginUser(
           @Valid @RequestBody LoginUserDto dto
            ) {
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(this.authService.login(dto));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<BaseResponseDto<AuthResponseDto>> verifyOtp(
           @Valid @RequestBody VerifyOtpDto dto
            ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(this.authService.verifyOtp(dto));
    }

    @GetMapping("/resend-otp")
    public ResponseEntity<BaseResponseDto<Object>> resendOtp(
            @RequestParam String email
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(this.authService.resendOtp(email, EmailPurpose.resend_otp));
    }

    @GetMapping("/forgot-password")
    public  ResponseEntity<BaseResponseDto<Object>> forgotPassword(
            @RequestParam String email
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(this.authService.resendOtp(email, EmailPurpose.forgot_password));
    }

    @PutMapping("/reset-password")
    public ResponseEntity<BaseResponseDto<Object>> resetPassword(
            @Valid @RequestBody  ResetPasswordDto dto
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(this.authService.resetPassword(dto));
    }
}
