package com.precious.finance_tracker.controllers;

import com.precious.finance_tracker.dtos.BaseResponseDto;
import com.precious.finance_tracker.entities.User;
import com.precious.finance_tracker.services.S3UploadService;
import com.precious.finance_tracker.services.interfaces.IUserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
@Tag(name = "Uploads")
@SecurityRequirement(name = "bearerAuth")
public class S3FileUploadController {
    private final S3UploadService s3UploadService;
    private final IUserService userService;

    @PostMapping(path = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponseDto<Object>> uploadAvatarToS3(
            @RequestParam("file") MultipartFile file) {
        User user = this.userService.getAuthenticatedUser();
        return ResponseEntity.ok(this.s3UploadService.uploadImageFileToS3(file, user.getId()));
    }

    @PostMapping(path = "/docs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponseDto<Object>> uploadDocumentToS3(
            @RequestParam("file") MultipartFile file) {
        User user = this.userService.getAuthenticatedUser();
        return ResponseEntity.ok(this.s3UploadService.uploadDocumentFileToS3(file, user.getId()));
    }
}
