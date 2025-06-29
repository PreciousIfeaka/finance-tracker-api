package com.precious.finance_tracker.services;

import com.precious.finance_tracker.dtos.email.VerifyEmailDto;
import com.precious.finance_tracker.enums.EmailPurpose;
import com.precious.finance_tracker.services.interfaces.IEmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.HashMap;
import java.util.Map;

@Service
@Data
public class EmailService implements IEmailService {
    private static Logger log = LoggerFactory.getLogger(EmailService.class.getName());

    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;

    @Async
    public void sendOtpEmailAsync(VerifyEmailDto dto) {
        try {
            log.info("OTP: {}", dto.getOtp());
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);

            Context context = new Context();
            context.setVariable("name", dto.getFirstName());
            context.setVariable("otp", dto.getOtp());

            Map<EmailPurpose, String[]> emailDataMap = new HashMap<>();
            emailDataMap.put(EmailPurpose.email_verification, new String[]{"Verify your email", "email-verification.html"});
            emailDataMap.put(EmailPurpose.resend_otp, new String[]{"Resend OTP", "resend-otp.html"});
            emailDataMap.put(EmailPurpose.forgot_password, new String[]{"Forgot Password", "forgot-password.html"});


            String htmlContent = templateEngine.process(emailDataMap.get(dto.getPurpose())[1], context);

            helper.setTo(dto.getRecipientEmail());
            helper.setSubject(emailDataMap.get(dto.getPurpose())[0]);
            helper.setText(htmlContent, true);

            javaMailSender.send(mimeMessage);

            log.info("Successfully sent mail to {} with otp {}", dto.getRecipientEmail(), dto.getOtp());
        } catch (MessagingException e) {
            log.error("Failed to send email to {}", dto.getRecipientEmail(), e);
        }
    }

    public String generateOtp() {
        return String.valueOf((int)(Math.random() * 900000) + 100000);
    }
}
