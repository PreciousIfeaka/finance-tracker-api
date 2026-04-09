package com.precious.finance_tracker.services;

import com.precious.finance_tracker.dtos.BaseResponseDto;
import com.precious.finance_tracker.exceptions.BadRequestException;
import com.precious.finance_tracker.exceptions.InternalServerError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3UploadServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private S3UploadService s3UploadService;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(s3UploadService, "s3BucketName", "test-bucket");
    }

    @Test
    void uploadImageFileToS3_ShouldUploadSuccessfully() throws IOException {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(1024L);
        when(multipartFile.getContentType()).thenReturn("image/jpeg");
        when(multipartFile.getOriginalFilename()).thenReturn("profile.jpg");
        when(multipartFile.getBytes()).thenReturn("dummy image data".getBytes());

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        BaseResponseDto<Object> response = s3UploadService.uploadImageFileToS3(multipartFile, userId);

        assertEquals("Success", response.getStatus());
        assertNotNull(response.getData());
        assertTrue(((Map<?, ?>) response.getData()).containsKey("fileKey"));
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void uploadImageFileToS3_ShouldThrowBadRequest_WhenFileIsEmpty() {
        when(multipartFile.isEmpty()).thenReturn(true);

        assertThrows(BadRequestException.class, () -> s3UploadService.uploadImageFileToS3(multipartFile, userId));
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void uploadImageFileToS3_ShouldThrowBadRequest_WhenInvalidType() {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(1024L);
        when(multipartFile.getContentType()).thenReturn("application/json");

        assertThrows(BadRequestException.class, () -> s3UploadService.uploadImageFileToS3(multipartFile, userId));
    }

    @Test
    void uploadDocumentFileToS3_ShouldUploadSuccessfully() throws IOException {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(1024L);
        when(multipartFile.getContentType()).thenReturn("application/pdf");
        when(multipartFile.getOriginalFilename()).thenReturn("statement.pdf");
        when(multipartFile.getBytes()).thenReturn("dummy doc data".getBytes());

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        BaseResponseDto<Object> response = s3UploadService.uploadDocumentFileToS3(multipartFile, userId);

        assertEquals("Success", response.getStatus());
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void uploadDocumentFileToS3_ShouldThrowInternalServerError_WhenS3Fails() throws IOException {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(1024L);
        when(multipartFile.getContentType()).thenReturn("application/pdf");
        when(multipartFile.getOriginalFilename()).thenReturn("statement.pdf");
        when(multipartFile.getBytes()).thenReturn("dummy doc data".getBytes());

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(new RuntimeException("S3 Failed"));

        assertThrows(InternalServerError.class, () -> s3UploadService.uploadDocumentFileToS3(multipartFile, userId));
    }

    @Test
    void generatePresignedGetUrl_ShouldReturnUrl() throws MalformedURLException {
        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        when(presignedRequest.url()).thenReturn(new URL("https://s3.amazonaws.com/test-bucket/test-key"));
        when(s3Presigner.presignGetObject(
                any(software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest.class)))
                .thenReturn(presignedRequest);

        String url = s3UploadService.generatePresignedGetUrl("test-key");

        assertNotNull(url);
        assertTrue(url.contains("test-key"));
    }

    @Test
    void deleteFromS3_ShouldDeleteSuccessfully() {
        // Will do nothing by default for void
        s3UploadService.deleteFromS3("test-key");

        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }
}
