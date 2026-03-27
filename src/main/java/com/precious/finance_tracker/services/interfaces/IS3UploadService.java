package com.precious.finance_tracker.services.interfaces;

import com.precious.finance_tracker.dtos.BaseResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface IS3UploadService {
    BaseResponseDto<Object> uploadImageFileToS3(
            MultipartFile file
    );

    BaseResponseDto<Object> uploadDocumentFileToS3(
            MultipartFile file
    );

    void deleteFromS3(String fileUrl);

}
