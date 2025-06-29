package com.precious.finance_tracker.dtos.user;

import com.precious.finance_tracker.enums.Currency;
import lombok.Data;

@Data
public class UpdateUserRequestDto {
    private final String name;

    private final String avatarUrl;

    private final Currency currency;
}
