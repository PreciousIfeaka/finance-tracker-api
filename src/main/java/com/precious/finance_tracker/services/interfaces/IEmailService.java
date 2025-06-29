package com.precious.finance_tracker.services.interfaces;

import com.precious.finance_tracker.dtos.email.VerifyEmailDto;

public interface IEmailService {
    void sendOtpEmailAsync(VerifyEmailDto dto);

    String generateOtp();
}
