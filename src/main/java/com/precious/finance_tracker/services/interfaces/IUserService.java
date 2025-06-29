package com.precious.finance_tracker.services.interfaces;

import com.precious.finance_tracker.dtos.BaseResponseDto;
import com.precious.finance_tracker.dtos.auth.RegisterUserDto;
import com.precious.finance_tracker.dtos.user.ChangePasswordDto;
import com.precious.finance_tracker.dtos.user.PagedUserResponseDto;
import com.precious.finance_tracker.dtos.user.UpdateUserRequestDto;
import com.precious.finance_tracker.dtos.user.UserResponseDto;
import com.precious.finance_tracker.entities.User;

public interface IUserService {
    public User createUser(RegisterUserDto dto);

    public UserResponseDto getUser(Object identifier);

    public PagedUserResponseDto getUsers(int page, int limit);

    public BaseResponseDto<UserResponseDto> updateUserDetails(UpdateUserRequestDto dto);

    public BaseResponseDto<UserResponseDto> changePassword(ChangePasswordDto dto);

    public BaseResponseDto<Object> deleteUserData();

    public User getAuthenticatedUser();
}
