package com.precious.finance_tracker.services;

import com.precious.finance_tracker.dtos.BaseResponseDto;
import com.precious.finance_tracker.dtos.auth.RegisterUserDto;
import com.precious.finance_tracker.dtos.user.ChangePasswordDto;
import com.precious.finance_tracker.dtos.user.PagedUserResponseDto;
import com.precious.finance_tracker.dtos.user.UpdateUserRequestDto;
import com.precious.finance_tracker.dtos.user.UserResponseDto;
import com.precious.finance_tracker.entities.User;
import com.precious.finance_tracker.enums.Currency;
import com.precious.finance_tracker.exceptions.BadRequestException;
import com.precious.finance_tracker.exceptions.NotFoundException;
import com.precious.finance_tracker.repositories.UserRepository;
import com.precious.finance_tracker.services.interfaces.IEmailService;
import com.precious.finance_tracker.services.interfaces.IS3UploadService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private IEmailService emailService;
    @Mock
    private IS3UploadService s3UploadService;

    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserService userService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(UUID.randomUUID())
                .name("John Doe")
                .email("john@example.com")
                .password("encoded_password")
                .currency(Currency.NGN)
                .isVerified(true)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockAuthentication() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("john@example.com");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));
    }

    @Test
    void createUser_ShouldReturnNewUser_WhenUserDoesNotExist() {
        RegisterUserDto dto = new RegisterUserDto();
        dto.setFirstName("John");
        dto.setLastName("Doe");
        dto.setEmail("john.doe@test.com");
        dto.setPassword("password123");
        dto.setConfirmPassword("password123");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(emailService.generateOtp()).thenReturn("123456");
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

        User result = userService.createUser(dto);

        assertNotNull(result);
        assertEquals("John Doe", result.getName());
        assertEquals("john.doe@test.com", result.getEmail());
        assertEquals("123456", result.getOtp());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_ShouldThrowBadRequest_WhenPasswordsMismatch() {
        RegisterUserDto dto = new RegisterUserDto();
        dto.setFirstName("John");
        dto.setLastName("Doe");
        dto.setEmail("john.doe@test.com");
        dto.setPassword("password123");
        dto.setConfirmPassword("password321");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> userService.createUser(dto));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getUser_ShouldReturnUser_WhenValidEmailProvided() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));

        UserResponseDto result = userService.getUser("john@example.com");

        assertNotNull(result);
        assertEquals("john@example.com", result.getEmail());
    }

    @Test
    void getUser_ShouldThrowNotFound_WhenInvalidEmailProvided() {
        when(userRepository.findByEmail("invalid@test.com")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.getUser("invalid@test.com"));
    }

    @Test
    void getUsers_ShouldReturnPagedResponse() {
        Page<User> page = new PageImpl<>(List.of(mockUser));
        when(userRepository.findAll(any(PageRequest.class))).thenReturn(page);

        PagedUserResponseDto result = userService.getUsers(0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals("john@example.com", result.getUsers().get(0).getEmail());
    }

    @Test
    void updateUserDetails_ShouldUpdateUserSuccessfully() {
        mockAuthentication();
        UpdateUserRequestDto dto = new UpdateUserRequestDto("Jane Doe", "http://new.avatar", Currency.USD);

        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        BaseResponseDto<UserResponseDto> result = userService.updateUserDetails(dto);

        assertNotNull(result);
        assertEquals("Success", result.getStatus());
        verify(userRepository).save(mockUser);
        assertEquals("Jane Doe", mockUser.getName());
        assertEquals(Currency.USD, mockUser.getCurrency());
    }

    @Test
    void changePassword_ShouldUpdatePassword() {
        mockAuthentication();
        ChangePasswordDto dto = new ChangePasswordDto("newPass", "newPass");

        when(passwordEncoder.matches("newPass", "encoded_password")).thenReturn(false);
        when(passwordEncoder.encode("newPass")).thenReturn("new_encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        BaseResponseDto<UserResponseDto> result = userService.changePassword(dto);

        assertEquals("Success", result.getStatus());
        verify(userRepository).save(mockUser);
        assertEquals("new_encoded_password", mockUser.getPassword());
    }

    @Test
    void changePassword_ShouldThrowBadRequest_WhenPasswordsMismatch() {
        mockAuthentication();
        ChangePasswordDto dto = new ChangePasswordDto("newPass", "mismatchPass");

        assertThrows(BadRequestException.class, () -> userService.changePassword(dto));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void deleteUserData_ShouldDeleteUser() {
        mockAuthentication();

        BaseResponseDto<Object> result = userService.deleteUserData();

        assertEquals("Success", result.getStatus());
        verify(userRepository).deleteById(mockUser.getId());
    }
}
