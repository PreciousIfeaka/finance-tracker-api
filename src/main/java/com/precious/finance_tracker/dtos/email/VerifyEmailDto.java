package com.precious.finance_tracker.dtos.email;

import com.precious.finance_tracker.enums.EmailPurpose;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VerifyEmailDto {
    private String recipientEmail;

    private String firstName;

    private String otp;

    private EmailPurpose purpose;
}
