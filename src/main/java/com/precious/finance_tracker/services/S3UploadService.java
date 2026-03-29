package com.precious.finance_tracker.services;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.precious.finance_tracker.dtos.BaseResponseDto;
import com.precious.finance_tracker.exceptions.BadRequestException;
import com.precious.finance_tracker.exceptions.InternalServerError;
import com.precious.finance_tracker.services.interfaces.IS3UploadService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class S3UploadService implements IS3UploadService {
    private static final Logger log = LoggerFactory.getLogger(S3UploadService.class.getName());

    private final int presignedUrlExpiryMins = 10;

    @Value("${s3.bucket-name}")
    private String s3BucketName;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    private final Cache<String, String> presignedUrlCache = Caffeine.newBuilder()
            .expireAfterWrite(presignedUrlExpiryMins - 1, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    public BaseResponseDto<Object> uploadImageFileToS3(
            MultipartFile file,
            UUID userId
    ) {
        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        List<String> allowedTypes = new ArrayList<>(List.of("image/"));

        this.validateFile(file, allowedTypes);

        String key = "image/%s".formatted(
                sanitizeFilename(userId, Objects.requireNonNull(file.getOriginalFilename()))
        );

        log.info("Successfully uploaded image to s3");
        return this.uploadToS3(file, key);
    }

    public BaseResponseDto<Object> uploadDocumentFileToS3(
            MultipartFile file,
            UUID userId
    ) {
        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        List<String> allowedTypes = new ArrayList<>(Arrays.asList(
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "text/plain",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "text/csv"
        ));

        this.validateFile(file, allowedTypes);

        String key = "docs/%s".formatted(
            sanitizeFilename(userId, Objects.requireNonNull(file.getOriginalFilename()))
        );

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
                .serverSideEncryption(ServerSideEncryption.AWS_KMS)
                .build();

        try {
            this.s3Client.putObject(putRequest, RequestBody.fromBytes(file.getBytes()));
        } catch (Exception e) {
            log.error("Error occurred while uploading file to s3", e);
            throw new InternalServerError("Error occurred while uploading file");
        }

        return BaseResponseDto.builder()
                .status("Success")
                .message("Successfully uploaded file to s3 bucket")
                .data(Map.of(
                        "fileKey", objectKey,
                        "mimeType", Objects.requireNonNull(file.getContentType())
                ))
                .build();
    }

    @Override
    public String generatePresignedGetUrl(String objectKey) {
        return presignedUrlCache.get(objectKey, key -> {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(s3BucketName)
                    .key(key)
                    .build();

            return s3Presigner.presignGetObject(
                    GetObjectPresignRequest.builder()
                            .signatureDuration(Duration.ofMinutes(presignedUrlExpiryMins))
                            .getObjectRequest(getObjectRequest)
                            .build()
            ).url().toString();
        });
    }

    @Override
    public String generatePresignedPutUrl(String objectKey, String contentType) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3BucketName)
                .key(objectKey)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignedUrlExpiryMins))
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        return presignedRequest.url().toString();
    }

    public void deleteFromS3(String objectKey) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(s3BucketName)
                    .key(objectKey)
                    .build();

            this.s3Client.deleteObject(deleteObjectRequest);

            this.evictPresignedUrl(objectKey);
            log.info("Successfully deleted file from s3");
        } catch (Exception e) {
            log.error("Error occurred while deleting file from s3", e);
            throw new InternalServerError("Error occurred while deleting file");
        }
    }

    public void evictPresignedUrl(String objectKey) {
        presignedUrlCache.invalidate(objectKey);
    }

    public void evictAllPresignedUrls() {
        presignedUrlCache.invalidateAll();
    }

    private String sanitizeFilename(UUID userId, String filename) {
        int lastIdx = filename.lastIndexOf('.');

        if (lastIdx == -1) {
            return filename.toLowerCase()
                    .replaceAll("[^a-z0-9-_.]", "");
        }

        String extension = filename.substring(lastIdx + 1);

        return  "%s/%s.%s".formatted(
                userId, UUID.randomUUID(), extension
        );
    }

    private void validateFile(MultipartFile file, List<String> allowedTypes) {
        int maxSize = 10 * 1024 * 1024;
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
