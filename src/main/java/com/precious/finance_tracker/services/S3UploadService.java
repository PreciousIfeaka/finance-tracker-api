package com.precious.finance_tracker.services;

import com.precious.finance_tracker.configurations.S3Config;
import com.precious.finance_tracker.dtos.BaseResponseDto;
import com.precious.finance_tracker.services.interfaces.IS3UploadService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Map;

@Service
@Data
public class S3UploadService implements IS3UploadService {
    @Value("${s3.bucket-name}")
    private String s3BucketName;

    private final S3Client s3Client;

    public BaseResponseDto<Object> uploadImageFileToSupaBaseS3(
            MultipartFile file,
            String objectKey
    ) throws IOException {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(s3BucketName)
                .key(objectKey)
                .contentType(file.getContentType())
                .build();

        this.s3Client.putObject(putRequest, RequestBody.fromBytes(file.getBytes()));

        String fileUrl = "https://" + s3BucketName + ".s3.amazonaws.com/" + objectKey;

        return BaseResponseDto.builder()
                .status("Success")
                .message("Successfully uploaded file to s3 bucket")
                .data(Map.of("fileUrl", fileUrl))
                .build();
    }
}
