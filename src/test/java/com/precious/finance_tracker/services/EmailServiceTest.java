package com.precious.finance_tracker.services;

import com.precious.finance_tracker.dtos.email.NotifyStatementProcessingResultDto;
import com.precious.finance_tracker.dtos.email.VerifyEmailDto;
import com.precious.finance_tracker.enums.EmailPurpose;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.jobrunr.jobs.lambdas.JobLambda;
import org.jobrunr.scheduling.JobScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private JobScheduler jobScheduler;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "senderEmail", "noreply@example.com");
    }

    @Test
    void sendOtpEmailAsync_ShouldEnqueueJob() {
        VerifyEmailDto dto = new VerifyEmailDto();
        dto.setRecipientEmail("test@example.com");

        emailService.sendOtpEmailAsync(dto);

        verify(jobScheduler).enqueue(any(JobLambda.class));
    }

    @Test
    void buildAndSendOtpEmail_ShouldSendEmail() throws MessagingException {
        VerifyEmailDto dto = VerifyEmailDto.builder()
                .firstName("John")
                .recipientEmail("john@example.com")
                .otp("123456")
                .purpose(EmailPurpose.email_verification)
                .build();

        MimeMessage mockMimeMessage = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mockMimeMessage);
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>Email Content</html>");

        emailService.buildAndSendOtpEmail(dto);

        verify(javaMailSender).send(mockMimeMessage);
    }

    @Test
    void buildAndSendOtpEmail_ShouldPropagateException_WhenSendFails() throws MessagingException {
        VerifyEmailDto dto = VerifyEmailDto.builder()
                .firstName("John")
                .recipientEmail("john@example.com")
                .otp("123456")
                .purpose(EmailPurpose.email_verification)
                .build();

        MimeMessage mockMimeMessage = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mockMimeMessage);
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>Email Content</html>");
        doThrow(new RuntimeException("Mail send failed")).when(javaMailSender).send(any(MimeMessage.class));

        assertThrows(RuntimeException.class, () -> emailService.buildAndSendOtpEmail(dto));
    }

    @Test
    void sendStatementEmailAsync_ShouldEnqueueJob() {
        NotifyStatementProcessingResultDto dto = new NotifyStatementProcessingResultDto();
        dto.setRecipientEmail("test@example.com");

        emailService.sendStatementEmailAsync(dto);

        verify(jobScheduler).enqueue(any(JobLambda.class));
    }

    @Test
    void buildAndSendStatementEmail_ShouldSendEmail() {
        NotifyStatementProcessingResultDto dto = new NotifyStatementProcessingResultDto();
        dto.setName("John");
        dto.setRecipientEmail("john@example.com");
        dto.setPurpose(EmailPurpose.statement_processing_success);

        MimeMessage mockMimeMessage = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mockMimeMessage);
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>Statement Processed</html>");

        emailService.buildAndSendStatementEmail(dto);

        verify(javaMailSender).send(mockMimeMessage);
    }

    @Test
    void generateOtp_ShouldReturnSixDigitNumberString() {
        String otp = emailService.generateOtp();

        assertNotNull(otp);
        assertEquals(6, otp.length());
        assertTrue(otp.matches("\\d{6}"));
    }
}
