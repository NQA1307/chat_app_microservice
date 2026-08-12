package com.discordclone.notificationservice.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailSenderService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    public void sendEmailVerificationCode(String to, String code, int expiresInMinutes) {
        sendHtmlCode(
                to,
                "Verify your email",
                "email/verify-email",
                code,
                expiresInMinutes
        );
    }

    public void sendPasswordResetCode(String to, String code, int expiresInMinutes) {
        sendHtmlCode(
                to,
                "Reset your password",
                "email/reset-password",
                code,
                expiresInMinutes
        );
    }

    private void sendHtmlCode(
            String to,
            String subject,
            String templateName,
            String code,
            int expiresInMinutes
    ) {
        Context context = new Context();
        context.setVariable("code", code);
        context.setVariable("expiresInMinutes", expiresInMinutes);

        String html = templateEngine.process(templateName, context);

        try {
            log.info("Sending auth email. to={}, subject={}", to, subject);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            if (mailUsername != null && !mailUsername.isBlank()) {
                helper.setFrom(mailUsername);
            }
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Auth email sent successfully. to={}, subject={}", to, subject);
        } catch (MessagingException e) {
            throw new IllegalStateException("Failed to build email message", e);
        } catch (MailException e) {
            log.error("Failed to send auth email. to={}, subject={}, error={}", to, subject, e.getMessage());
            throw e;
        }
    }
}
