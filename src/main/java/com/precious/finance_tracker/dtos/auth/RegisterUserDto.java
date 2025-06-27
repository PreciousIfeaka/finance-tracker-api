package com.precious.finance_tracker.dtos.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@AllArgsConstructor
public class RegisterUserDto {
    @NotBlank(message = "firstName is required")
    private final String firstName;

    @NotBlank(message = "lastName is required")
    private final String lastName;

    @Email(message = "email must be a valid email format")
    private final String email;

    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "password must be at least 8 characters long containing uppercase, lowercase, number & special character"
    )
    private final String password;

    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "confirmPassword must be at least 8 characters long containing uppercase, lowercase, number & special character"
    )
    private final String confirmPassword;
}
