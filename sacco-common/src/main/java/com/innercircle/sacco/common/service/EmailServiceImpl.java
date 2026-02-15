package com.innercircle.sacco.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${sacco.mail.from}")
    private String fromEmail;

    @Value("${sacco.mail.password-reset-url}")
    private String passwordResetUrl;

    @Override
    public void sendPasswordResetEmail(String to, String token) {
        String subject = "Password Reset Request - InnerCircle SACCO";
        String resetLink = passwordResetUrl + "?token=" + token;
        String body = String.format(
            "Hello,\n\n" +
            "You have requested to reset your password for your InnerCircle SACCO account.\n\n" +
            "Please click the link below to reset your password:\n" +
            "%s\n\n" +
            "This link will expire in 24 hours.\n\n" +
            "If you did not request this password reset, please ignore this email.\n\n" +
            "Best regards,\n" +
            "InnerCircle SACCO Team",
            resetLink
        );

        sendGenericEmail(to, subject, body);
        log.info("Password reset email sent to: {}", to);
    }

    @Override
    public void sendWelcomeEmail(String to, String username) {
        String subject = "Welcome to InnerCircle SACCO";
        String body = String.format(
            "Hello %s,\n\n" +
            "Welcome to InnerCircle SACCO!\n\n" +
            "Your account has been successfully created. You can now log in and start using our services.\n\n" +
            "If you have any questions or need assistance, please don't hesitate to contact us.\n\n" +
            "Best regards,\n" +
            "InnerCircle SACCO Team",
            username
        );

        sendGenericEmail(to, subject, body);
        log.info("Welcome email sent to: {}", to);
    }

    @Override
    public void sendGenericEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.debug("Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to: {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
