package com.precious.finance_tracker.services;

import com.precious.finance_tracker.dtos.BaseResponseDto;
import com.precious.finance_tracker.exceptions.BadRequestException;
import com.precious.finance_tracker.exceptions.InternalServerError;
import com.precious.finance_tracker.services.interfaces.IS3UploadService;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.*;

@Service
@RequiredArgsConstructor
public class S3UploadService implements IS3UploadService {
    private static final Logger log = LoggerFactory.getLogger(S3UploadService.class.getName());

    @Value("${s3.bucket-name}")
    private String s3BucketName;
    private final S3Client s3Client;
    private final int maxSize = 10 * 1024 * 1024;

    public BaseResponseDto<Object> uploadImageFileToS3(
            MultipartFile file
    ) {
        List<String> allowedTypes = new ArrayList<>(List.of("image/"));

        this.validateFile(file, allowedTypes, maxSize);

        String key = "image/" + UUID.randomUUID().toString().replace("-", "") +
                this.sanitizeFilename(Objects.requireNonNull(file.getOriginalFilename()));

        log.info("Successfully uploaded image to s3");
        return this.uploadToS3(file, key);
    }

    public BaseResponseDto<Object> uploadDocumentFileToS3(
            MultipartFile file
    ) {
        List<String> allowedTypes = new ArrayList<>(Arrays.asList(
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "text/plain",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "text/csv"
        ));

        this.validateFile(file, allowedTypes, maxSize);

        String key = "docs/" + UUID.randomUUID().toString().replace("-", "") +
                this.sanitizeFilename(Objects.requireNonNull(file.getOriginalFilename()));

        log.info("Successfully uploaded document file to s3");
        return this.uploadToS3(file, key);
    }

    private BaseResponseDto<Object> uploadToS3(
            MultipartFile file,
            String objectKey
    ) {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(s3BucketName)
                .key(objectKey)
                .contentType(file.getContentType())
                .build();

        try {
            this.s3Client.putObject(putRequest, RequestBody.fromBytes(file.getBytes()));
        } catch (Exception e) {
            log.error("Error occurred while uploading file to s3", e);
            throw new InternalServerError("Error occurred while uploading file");
        }

        String fileUrl = "https://" + s3BucketName + ".s3.amazonaws.com/" + objectKey;

        return BaseResponseDto.builder()
                .status("Success")
                .message("Successfully uploaded file to s3 bucket")
                .data(Map.of("fileUrl", fileUrl, "mimeType", Objects.requireNonNull(file.getContentType())))
                .build();
    }

    public void deleteFromS3(String fileUrl) {
        String baseUrl = "https://" + s3BucketName + ".s3.amazonaws.com/";
        String objectKey = fileUrl.replace(baseUrl, "");

        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(s3BucketName)
                    .key(objectKey)
                    .build();

            this.s3Client.deleteObject(deleteObjectRequest);
            log.info("Successfully deleted file from s3");
        } catch (Exception e) {
            log.error("Error occurred while deleting file from s3", e);
            throw new InternalServerError("Error occurred while deleting file");
        }
    }

    private String sanitizeFilename(String filename) {
        int lastIdx = filename.lastIndexOf('.');

        if (lastIdx == -1) {
            return filename.toLowerCase()
                    .replaceAll("[^a-z0-9-_.]", "");
        }

        String name = filename.substring(0, lastIdx)
                .toLowerCase()
                .replaceAll("[^a-z0-9-_.]", "");

        String extension = filename.substring(lastIdx + 1);

        return name + "." + extension;
    }

    private void validateFile(MultipartFile file, List<String> allowedTypes, int maxSize) {
        if (file.getSize() > maxSize) {
            throw new BadRequestException(
                    "File size exceeds limit of " + (maxSize / (1024 * 1024)) + "MB"
            );
        }

        String contentType = file.getContentType();

        if (contentType == null || contentType.isBlank()) {
            throw new BadRequestException("File type is missing");
        }

        boolean isAllowed = allowedTypes.stream()
                .anyMatch(type ->
                        type.endsWith("/")
                                ? contentType.startsWith(type)
                                : contentType.equals(type)
                        );

        if (!isAllowed) {
            throw new BadRequestException("File type " + contentType + " is not allowed");
        }
    }
}
