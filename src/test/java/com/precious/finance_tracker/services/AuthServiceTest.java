package com.precious.finance_tracker.services;

import com.precious.finance_tracker.configurations.JwtService;
import com.precious.finance_tracker.dtos.BaseResponseDto;
import com.precious.finance_tracker.dtos.auth.*;
import com.precious.finance_tracker.dtos.email.VerifyEmailDto;
import com.precious.finance_tracker.dtos.user.UserResponseDto;
import com.precious.finance_tracker.entities.User;
import com.precious.finance_tracker.exceptions.BadRequestException;
import com.precious.finance_tracker.exceptions.ForbiddenException;
import com.precious.finance_tracker.repositories.UserRepository;
import com.precious.finance_tracker.services.interfaces.IEmailService;
import com.precious.finance_tracker.services.interfaces.IS3UploadService;
import com.precious.finance_tracker.services.interfaces.IUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private IUserService userService;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private IEmailService emailService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private IS3UploadService s3UploadService;

    @InjectMocks
    private AuthService authService;

    private User mockUser;
    private UserResponseDto mockUserResponseDto;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(UUID.randomUUID())
                .name("Jane Doe")
                .email("jane@example.com")
                .password("encoded_password")
                .otp("123456")
                .otpExpiredAt(LocalDateTime.now().plusMinutes(10))
                .isVerified(false)
                .build();

        mockUserResponseDto = UserResponseDto.builder()
                .id(mockUser.getId())
                .name(mockUser.getName())
                .email(mockUser.getEmail())
                .currency(mockUser.getCurrency())
                .isVerified(mockUser.getIsVerified())
                .role(mockUser.getRole())
                .avatarUrl("http://avatar.url")
                .build();
    }

    @Test
    void registerUser_ShouldReturnSuccess_WhenUserNotVerified() {
        RegisterUserDto dto = new RegisterUserDto();
        dto.setFirstName("Jane");
        dto.setEmail("jane@example.com");

        when(userService.createUser(dto)).thenReturn(mockUser);

        BaseResponseDto<UserResponseDto> result = authService.registerUser(dto);

        assertEquals("Success", result.getStatus());
        verify(emailService).sendOtpEmailAsync(any(VerifyEmailDto.class));
    }

    @Test
    void login_ShouldReturnToken_WhenCredentialsValidAndVerified() {
        LoginUserDto dto = new LoginUserDto();
        dto.setEmail("jane@example.com");
        dto.setPassword("password");

        mockUser.setIsVerified(true);
        UserResponseDto verifiedUserDto = UserResponseDto.builder()
                .id(mockUser.getId())
                .name(mockUser.getName())
                .email(mockUser.getEmail())
                .currency(mockUser.getCurrency())
                .isVerified(mockUser.getIsVerified())
                .role(mockUser.getRole())
                .avatarUrl("http://avatar.url")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mock(Authentication.class));
        when(userService.getUser("jane@example.com")).thenReturn(verifiedUserDto);
        when(jwtService.generateToken("jane@example.com")).thenReturn("access_token");

        BaseResponseDto<AuthResponseDto> result = authService.login(dto);

        assertEquals("Success", result.getStatus());
        assertEquals("access_token", result.getData().getAccessToken());
    }

    @Test
    void login_ShouldThrowForbidden_WhenUserNotVerified() {
        LoginUserDto dto = new LoginUserDto();
        dto.setEmail("jane@example.com");
        dto.setPassword("password");

        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(userService.getUser("jane@example.com")).thenReturn(mockUserResponseDto); // Not verified

        assertThrows(ForbiddenException.class, () -> authService.login(dto));
    }

    @Test
    void verifyOtp_ShouldReturnAuthResponse_WhenOtpValid() {
        VerifyOtpDto dto = new VerifyOtpDto();
        dto.setEmail("jane@example.com");
        dto.setOtp("123456");

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(mockUser));
        when(jwtService.generateToken("jane@example.com")).thenReturn("access_token");

        BaseResponseDto<AuthResponseDto> result = authService.verifyOtp(dto);

        assertEquals("Success", result.getStatus());
        assertEquals("access_token", result.getData().getAccessToken());
        assertNull(mockUser.getOtp());
        assertTrue(mockUser.getIsVerified());
        verify(userRepository).save(mockUser);
    }

    @Test
    void verifyOtp_ShouldThrowBadRequest_WhenOtpInvalid() {
        VerifyOtpDto dto = new VerifyOtpDto();
        dto.setEmail("jane@example.com");
        dto.setOtp("wrong_otp");

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(mockUser));

        assertThrows(BadRequestException.class, () -> authService.verifyOtp(dto));
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_ShouldUpdatePassword() {
        ResetPasswordDto dto = new ResetPasswordDto();
        dto.setOtp("123456");
        dto.setPassword("new_password");
        dto.setConfirmPassword("new_password");

        when(userRepository.findByOtp("123456")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.encode("new_password")).thenReturn("new_encoded_password");

        BaseResponseDto<Object> result = authService.resetPassword(dto);

        assertEquals("Success", result.getStatus());
        assertEquals("new_encoded_password", mockUser.getPassword());
        assertTrue(mockUser.getIsVerified());
        verify(userRepository).save(mockUser);
    }
}
