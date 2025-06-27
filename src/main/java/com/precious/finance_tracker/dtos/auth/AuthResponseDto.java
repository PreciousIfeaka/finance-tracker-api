package com.precious.finance_tracker.dtos.auth;

import com.precious.finance_tracker.dtos.user.UserResponseDto;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponseDto {
    private UserResponseDto user;

    @NotBlank
    private String accessToken;
}
