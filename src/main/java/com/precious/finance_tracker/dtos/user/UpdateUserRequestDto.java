package com.precious.finance_tracker.dtos.user;

import com.precious.finance_tracker.enums.Currency;
import lombok.Data;

public record UpdateUserRequestDto(String name, String avatarUrl, Currency currency) {
}
