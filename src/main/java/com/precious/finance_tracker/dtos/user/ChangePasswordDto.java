package com.precious.finance_tracker.dtos.user;

import jakarta.validation.constraints.Pattern;

public record ChangePasswordDto(
                @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$", message = "password must be at least 8 characters long containing uppercase, lowercase, number & special character") String password,
                @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$", message = "password must be at least 8 characters long containing uppercase, lowercase, number & special character") String confirmPassword) {
}
