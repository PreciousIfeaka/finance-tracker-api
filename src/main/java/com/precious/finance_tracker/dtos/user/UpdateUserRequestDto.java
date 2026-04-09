package com.precious.finance_tracker.dtos.user;

import com.precious.finance_tracker.enums.Currency;

public record UpdateUserRequestDto(String name, String avatarUrl, Currency currency) {
}
