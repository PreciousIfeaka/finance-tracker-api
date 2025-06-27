package com.precious.finance_tracker.dtos.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VerifyOtpDto {
    @Email(message = "Email is not a valid email format")
    private final String email;

    @NotBlank
    private final String otp;
}
