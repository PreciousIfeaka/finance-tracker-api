package com.precious.finance_tracker.services.interfaces;

import com.precious.finance_tracker.dtos.BaseResponseDto;
import com.precious.finance_tracker.dtos.auth.*;
import com.precious.finance_tracker.dtos.user.UserResponseDto;
import com.precious.finance_tracker.enums.EmailPurpose;

public interface IAuthService {
    BaseResponseDto<UserResponseDto> registerUser(RegisterUserDto dto);

    BaseResponseDto<AuthResponseDto> login(LoginUserDto dto);

    BaseResponseDto<AuthResponseDto> verifyOtp(VerifyOtpDto dto);

    BaseResponseDto<Object> resendOtp(String email, EmailPurpose purpose);

    BaseResponseDto<Object> resetPassword(ResetPasswordDto dto);
}
