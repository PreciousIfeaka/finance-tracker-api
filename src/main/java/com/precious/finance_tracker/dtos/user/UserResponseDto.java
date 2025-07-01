package com.precious.finance_tracker.dtos.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    private final UUID id;

    private final String name;

    private final String email;

    private final Currency currency;

    private final boolean isVerified;

    private final Roles role;

    private final String avatarUrl;

    @JsonIgnore
    private final String otp;

    private final LocalDateTime createdAt;

    private final LocalDateTime updatedAt;

    public static UserResponseDto fromEntity(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .isVerified(user.getIsVerified())
                .avatarUrl(user.getAvatarUrl())
                .role((user.getRole()))
                .currency(user.getCurrency())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
