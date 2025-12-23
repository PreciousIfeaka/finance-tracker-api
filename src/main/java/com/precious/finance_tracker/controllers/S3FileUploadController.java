package com.precious.finance_tracker.controllers;

import com.precious.finance_tracker.dtos.BaseResponseDto;
import com.precious.finance_tracker.services.S3UploadService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;


@RestController
@RequestMapping("/api/v1/uploads")
@Data
@Tag(name = "Uploads")
@SecurityRequirement(name = "bearerAuth")
public class S3FileUploadController {
    private final S3UploadService s3UploadService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponseDto<Object>> uploadFileToS3(
            @RequestParam("file") MultipartFile file
            ) throws IOException {
        String bucket = "Finance tracker";
        String key = "avatar/" + file.getOriginalFilename();

        return ResponseEntity.ok(this.s3UploadService.uploadImageFileToSupaBaseS3(file, bucket, key));
    }
}
