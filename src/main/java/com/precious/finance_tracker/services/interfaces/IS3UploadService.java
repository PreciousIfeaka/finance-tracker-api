package com.precious.finance_tracker.services.interfaces;

import com.precious.finance_tracker.dtos.BaseResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface IS3UploadService {
    BaseResponseDto<Object> uploadImageFileToS3(
            MultipartFile file,
            UUID userId
    );

    BaseResponseDto<Object> uploadDocumentFileToS3(
            MultipartFile file,
            UUID userId
    );

    void deleteFromS3(String fileUrl);

    String generatePresignedGetUrl(String objectKey);

    String generatePresignedPutUrl(String objectKey, String contentType);
}
