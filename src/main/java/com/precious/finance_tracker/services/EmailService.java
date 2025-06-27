package com.precious.finance_tracker.services;

import com.precious.finance_tracker.dtos.email.VerifyEmailDto;
import com.precious.finance_tracker.enums.EmailPurpose;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.Data;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.HashMap;
import java.util.Map;

@Service
@Data
public class EmailService {

    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;

    public void sendOtpEmail(VerifyEmailDto dto) throws MessagingException {
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
    }

    public String generateOtp() {
        return String.valueOf((int)(Math.random() * 900000) + 100000);
    }
}
