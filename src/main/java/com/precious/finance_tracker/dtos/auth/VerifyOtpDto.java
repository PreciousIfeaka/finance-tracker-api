package com.precious.finance_tracker.dtos.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyOtpDto {
    @Email(message = "Email is not a valid email format")
    private String email;

    @NotBlank
    private String otp;
}
