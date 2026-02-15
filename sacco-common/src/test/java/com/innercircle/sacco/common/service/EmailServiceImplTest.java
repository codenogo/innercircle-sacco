package com.innercircle.sacco.common.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailServiceImpl emailService;

    @Captor
    private ArgumentCaptor<SimpleMailMessage> messageCaptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@innercircle.com");
        ReflectionTestUtils.setField(emailService, "passwordResetUrl", "https://innercircle.com/reset");
    }

    // --- sendPasswordResetEmail tests ---

    @Test
    void sendPasswordResetEmail_shouldSendEmailWithCorrectRecipient() {
        emailService.sendPasswordResetEmail("user@example.com", "reset-token-123");

        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage sent = messageCaptor.getValue();

        assertThat(sent.getTo()).containsExactly("user@example.com");
    }

    @Test
    void sendPasswordResetEmail_shouldSendEmailWithCorrectSubject() {
        emailService.sendPasswordResetEmail("user@example.com", "token");

        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage sent = messageCaptor.getValue();

        assertThat(sent.getSubject()).isEqualTo("Password Reset Request - InnerCircle SACCO");
    }

    @Test
    void sendPasswordResetEmail_shouldIncludeResetLinkInBody() {
        emailService.sendPasswordResetEmail("user@example.com", "my-token-xyz");

        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage sent = messageCaptor.getValue();

        assertThat(sent.getText()).contains("https://innercircle.com/reset?token=my-token-xyz");
    }

    @Test
    void sendPasswordResetEmail_shouldSetFromAddress() {
        emailService.sendPasswordResetEmail("user@example.com", "token");

        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage sent = messageCaptor.getValue();

        assertThat(sent.getFrom()).isEqualTo("noreply@innercircle.com");
    }

    @Test
    void sendPasswordResetEmail_bodyContainsExpiryNotice() {
        emailService.sendPasswordResetEmail("user@example.com", "token");

        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage sent = messageCaptor.getValue();

        assertThat(sent.getText()).contains("This link will expire in 24 hours");
    }

    @Test
    void sendPasswordResetEmail_bodyContainsIgnoreNotice() {
        emailService.sendPasswordResetEmail("user@example.com", "token");

        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage sent = messageCaptor.getValue();

        assertThat(sent.getText()).contains("If you did not request this password reset, please ignore this email");
    }

    // --- sendWelcomeEmail tests ---

    @Test
    void sendWelcomeEmail_shouldSendEmailWithCorrectRecipient() {
        emailService.sendWelcomeEmail("newuser@example.com", "JohnDoe");

        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage sent = messageCaptor.getValue();

        assertThat(sent.getTo()).containsExactly("newuser@example.com");
    }

    @Test
    void sendWelcomeEmail_shouldSendEmailWithCorrectSubject() {
        emailService.sendWelcomeEmail("newuser@example.com", "JohnDoe");

        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage sent = messageCaptor.getValue();

        assertThat(sent.getSubject()).isEqualTo("Welcome to InnerCircle SACCO");
    }

    @Test
    void sendWelcomeEmail_shouldIncludeUsernameInBody() {
        emailService.sendWelcomeEmail("newuser@example.com", "JohnDoe");

        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage sent = messageCaptor.getValue();

        assertThat(sent.getText()).contains("Hello JohnDoe");
    }

    @Test
    void sendWelcomeEmail_shouldSetFromAddress() {
        emailService.sendWelcomeEmail("newuser@example.com", "JohnDoe");

        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage sent = messageCaptor.getValue();

        assertThat(sent.getFrom()).isEqualTo("noreply@innercircle.com");
    }

    @Test
    void sendWelcomeEmail_bodyContainsWelcomeMessage() {
        emailService.sendWelcomeEmail("newuser@example.com", "JohnDoe");

        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage sent = messageCaptor.getValue();

        assertThat(sent.getText()).contains("Welcome to InnerCircle SACCO!");
        assertThat(sent.getText()).contains("Your account has been successfully created");
    }

    // --- sendGenericEmail tests ---

    @Test
    void sendGenericEmail_shouldSendEmailWithAllFieldsSet() {
        emailService.sendGenericEmail("recipient@example.com", "Test Subject", "Test Body");

        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage sent = messageCaptor.getValue();

        assertThat(sent.getTo()).containsExactly("recipient@example.com");
        assertThat(sent.getSubject()).isEqualTo("Test Subject");
        assertThat(sent.getText()).isEqualTo("Test Body");
        assertThat(sent.getFrom()).isEqualTo("noreply@innercircle.com");
    }

    @Test
    void sendGenericEmail_shouldCallMailSenderSend() {
        emailService.sendGenericEmail("to@test.com", "Subject", "Body");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    // --- Error handling tests ---

    @Test
    void sendGenericEmail_whenMailSenderThrows_shouldWrapInRuntimeException() {
        doThrow(new MailSendException("SMTP connection refused"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() ->
                emailService.sendGenericEmail("user@test.com", "Subject", "Body"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to send email")
                .hasCauseInstanceOf(MailSendException.class);
    }

    @Test
    void sendPasswordResetEmail_whenMailSenderThrows_shouldPropagateException() {
        doThrow(new MailSendException("Connection timeout"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() ->
                emailService.sendPasswordResetEmail("user@test.com", "token"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to send email");
    }

    @Test
    void sendWelcomeEmail_whenMailSenderThrows_shouldPropagateException() {
        doThrow(new MailSendException("Authentication failed"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() ->
                emailService.sendWelcomeEmail("user@test.com", "JaneDoe"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to send email");
    }
}
