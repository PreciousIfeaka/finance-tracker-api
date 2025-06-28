package com.precious.finance_tracker.dtos.user;

import com.precious.finance_tracker.enums.Currency;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateUserRequestDto {
    private final String name;

    private final Currency currency;
}
