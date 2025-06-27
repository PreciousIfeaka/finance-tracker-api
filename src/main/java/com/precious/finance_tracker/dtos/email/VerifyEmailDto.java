package com.precious.finance_tracker.dtos.email;

import com.precious.finance_tracker.enums.EmailPurpose;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VerifyEmailDto {
    private final String recipientEmail;

    private final String firstName;

    private final String otp;

    private final EmailPurpose purpose;
}
