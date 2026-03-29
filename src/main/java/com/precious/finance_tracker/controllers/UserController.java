package com.precious.finance_tracker.controllers;

import com.precious.finance_tracker.dtos.BaseResponseDto;
import com.precious.finance_tracker.dtos.user.ChangePasswordDto;
import com.precious.finance_tracker.dtos.user.PagedUserResponseDto;
import com.precious.finance_tracker.dtos.user.UpdateUserRequestDto;
import com.precious.finance_tracker.dtos.user.UserResponseDto;
import com.precious.finance_tracker.services.UserService;
import com.precious.finance_tracker.services.interfaces.IS3UploadService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final IS3UploadService s3UploadService;

    @GetMapping("/me")
    public ResponseEntity<BaseResponseDto<UserResponseDto>> getMyProfile() {
        UserResponseDto userResponseDto = UserResponseDto.fromEntity(
                this.userService.getAuthenticatedUser(),
                s3UploadService
        );

        return ResponseEntity.ok(
                BaseResponseDto.<UserResponseDto>builder()
                        .status("Success")
                        .message("Successfully retrieved user profile")
                        .data(userResponseDto)
                        .build()
        );
    }

    @GetMapping()
    public ResponseEntity<BaseResponseDto<PagedUserResponseDto>> getAllUsers(
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name= "limit", required = false, defaultValue = "10") int limit
    ) {
        PagedUserResponseDto users = this.userService.getUsers(page, limit);

        return ResponseEntity.ok(
                BaseResponseDto.<PagedUserResponseDto>builder()
                        .status("Success")
                        .message("Successfully retrieved users")
                        .data(users)
                        .build()
        );
    }

    @PutMapping("/update-profile")
    public ResponseEntity<BaseResponseDto<UserResponseDto>> updateUserProfile(
            @Valid @RequestBody UpdateUserRequestDto dto
    ) {
        return ResponseEntity.ok(this.userService.updateUserDetails(dto));
    }

    @PutMapping("/change-password")
    public ResponseEntity<BaseResponseDto<UserResponseDto>> changePassword(
            @Valid @RequestBody ChangePasswordDto dto
            ) {
        return ResponseEntity.ok(this.userService.changePassword(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponseDto<Object>> deleteMyAccount(
            @PathVariable("id") UUID id
            ) {
         return ResponseEntity.ok(this.userService.deleteUserData());
    }
}
