package com.precious.finance_tracker.dtos.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.precious.finance_tracker.entities.User;
import com.precious.finance_tracker.enums.Currency;
import com.precious.finance_tracker.enums.Roles;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class UserResponseDto {
    private UUID id;

    private String name;

    private String email;

    private Currency currency;

    private boolean isVerified;

    private Roles role;

    @JsonIgnore
    private String otp;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static UserResponseDto fromEntity(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .isVerified(user.getIsVerified())
                .role((user.getRole()))
                .currency(user.getCurrency())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
