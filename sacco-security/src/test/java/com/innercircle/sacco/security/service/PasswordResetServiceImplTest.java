package com.innercircle.sacco.security.service;

import com.innercircle.sacco.common.service.EmailService;
import com.innercircle.sacco.security.entity.PasswordResetToken;
import com.innercircle.sacco.security.entity.UserAccount;
import com.innercircle.sacco.security.repository.PasswordResetTokenRepository;
import com.innercircle.sacco.security.repository.UserAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetServiceImpl")
class PasswordResetServiceImplTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetServiceImpl passwordResetService;

    @Captor
    private ArgumentCaptor<PasswordResetToken> tokenCaptor;

    @Captor
    private ArgumentCaptor<UserAccount> userCaptor;

    private static final String VALID_PASSWORD = "StrongP@ss1";
    private static final String TEST_EMAIL = "user@example.com";
    private static final String TEST_USERNAME = "testuser";

    private UserAccount createTestUser() {
        UUID userId = UUID.randomUUID();
        UserAccount user = new UserAccount();
        user.setId(userId);
        user.setUsername(TEST_USERNAME);
        user.setEmail(TEST_EMAIL);
        user.setPassword("oldEncodedPassword");
        user.setEnabled(true);
        user.setAccountNonLocked(true);
        return user;
    }

    private PasswordResetToken createValidToken(UUID userId) {
        PasswordResetToken token = new PasswordResetToken();
        token.setId(UUID.randomUUID());
        token.setUserId(userId);
        token.setToken("valid-token-string");
        token.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        token.setUsed(false);
        return token;
    }

    @Nested
    @DisplayName("requestPasswordReset")
    class RequestPasswordReset {

        @Test
        @DisplayName("should generate token, save it, and send email when user exists")
        void shouldGenerateTokenAndSendEmail() {
            UserAccount user = createTestUser();
            when(userAccountRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));
            when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            passwordResetService.requestPasswordReset(TEST_EMAIL);

            verify(passwordResetTokenRepository).save(tokenCaptor.capture());
            PasswordResetToken savedToken = tokenCaptor.getValue();

            assertThat(savedToken.getUserId()).isEqualTo(user.getId());
            assertThat(savedToken.getToken()).isNotNull().isNotEmpty();
            assertThat(savedToken.getUsed()).isFalse();
            assertThat(savedToken.getExpiresAt()).isAfter(Instant.now());
            assertThat(savedToken.getExpiresAt()).isBefore(Instant.now().plus(25, ChronoUnit.HOURS));

            verify(emailService).sendPasswordResetEmail(eq(TEST_EMAIL), eq(savedToken.getToken()));
        }

        @Test
        @DisplayName("should generate Base64 URL-safe token from 32 bytes via SecureRandom")
        void shouldGenerateSecureToken() {
            UserAccount user = createTestUser();
            when(userAccountRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));
            when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            passwordResetService.requestPasswordReset(TEST_EMAIL);

            verify(passwordResetTokenRepository).save(tokenCaptor.capture());
            String token = tokenCaptor.getValue().getToken();

            // Base64 URL encoding of 32 bytes without padding = 43 chars
            assertThat(token).hasSize(43);
            // Should be URL-safe Base64 (no +, /, =)
            assertThat(token).doesNotContain("+", "/", "=");
        }

        @Test
        @DisplayName("should generate unique tokens on successive calls")
        void shouldGenerateUniqueTokens() {
            UserAccount user = createTestUser();
            when(userAccountRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));
            when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            passwordResetService.requestPasswordReset(TEST_EMAIL);
            passwordResetService.requestPasswordReset(TEST_EMAIL);

            verify(passwordResetTokenRepository, org.mockito.Mockito.times(2)).save(tokenCaptor.capture());
            var capturedTokens = tokenCaptor.getAllValues();

            assertThat(capturedTokens.get(0).getToken())
                    .isNotEqualTo(capturedTokens.get(1).getToken());
        }

        @Test
        @DisplayName("should silently return without sending email when email not found")
        void shouldSilentlyReturnForNonExistentEmail() {
            when(userAccountRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

            passwordResetService.requestPasswordReset("nonexistent@example.com");

            verifyNoInteractions(passwordResetTokenRepository);
            verifyNoInteractions(emailService);
        }

        @Test
        @DisplayName("should set token expiry to 24 hours from now")
        void shouldSetCorrectTokenExpiry() {
            UserAccount user = createTestUser();
            when(userAccountRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(user));
            when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Instant before = Instant.now().plus(24, ChronoUnit.HOURS);
            passwordResetService.requestPasswordReset(TEST_EMAIL);
            Instant after = Instant.now().plus(24, ChronoUnit.HOURS);

            verify(passwordResetTokenRepository).save(tokenCaptor.capture());
            Instant expiresAt = tokenCaptor.getValue().getExpiresAt();

            // The expiry time should be approximately 24 hours from now
            assertThat(expiresAt).isBetween(before.minusSeconds(1), after.plusSeconds(1));
        }
    }

    @Nested
    @DisplayName("resetPassword")
    class ResetPassword {

        @Test
        @DisplayName("should reset password successfully with valid token and valid password")
        void shouldResetPasswordSuccessfully() {
            UUID userId = UUID.randomUUID();
            PasswordResetToken resetToken = createValidToken(userId);
            UserAccount user = createTestUser();
            user.setId(userId);

            when(passwordResetTokenRepository.findByToken("valid-token-string"))
                    .thenReturn(Optional.of(resetToken));
            when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode(VALID_PASSWORD)).thenReturn("encodedNewPassword");
            when(userAccountRepository.save(any(UserAccount.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            passwordResetService.resetPassword("valid-token-string", VALID_PASSWORD);

            // Verify password was updated
            verify(userAccountRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getPassword()).isEqualTo("encodedNewPassword");

            // Verify token was marked as used
            verify(passwordResetTokenRepository).save(tokenCaptor.capture());
            assertThat(tokenCaptor.getValue().getUsed()).isTrue();
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when token not found")
        void shouldThrowWhenTokenNotFound() {
            when(passwordResetTokenRepository.findByToken("invalid-token"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> passwordResetService.resetPassword("invalid-token", VALID_PASSWORD))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid or expired token");
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when token already used")
        void shouldThrowWhenTokenAlreadyUsed() {
            UUID userId = UUID.randomUUID();
            PasswordResetToken usedToken = createValidToken(userId);
            usedToken.setUsed(true);

            when(passwordResetTokenRepository.findByToken("used-token"))
                    .thenReturn(Optional.of(usedToken));

            assertThatThrownBy(() -> passwordResetService.resetPassword("used-token", VALID_PASSWORD))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Token has already been used");

            verify(userAccountRepository, never()).findById(any());
            verify(userAccountRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when token has expired")
        void shouldThrowWhenTokenExpired() {
            UUID userId = UUID.randomUUID();
            PasswordResetToken expiredToken = createValidToken(userId);
            expiredToken.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));

            when(passwordResetTokenRepository.findByToken("expired-token"))
                    .thenReturn(Optional.of(expiredToken));

            assertThatThrownBy(() -> passwordResetService.resetPassword("expired-token", VALID_PASSWORD))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Token has expired");

            verify(userAccountRepository, never()).findById(any());
            verify(userAccountRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when user not found for token's userId")
        void shouldThrowWhenUserNotFoundForToken() {
            UUID userId = UUID.randomUUID();
            PasswordResetToken resetToken = createValidToken(userId);

            when(passwordResetTokenRepository.findByToken("valid-token-string"))
                    .thenReturn(Optional.of(resetToken));
            when(userAccountRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> passwordResetService.resetPassword("valid-token-string", VALID_PASSWORD))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("User not found");
        }

        @Test
        @DisplayName("should encode password using PasswordEncoder before saving")
        void shouldEncodePasswordBeforeSaving() {
            UUID userId = UUID.randomUUID();
            PasswordResetToken resetToken = createValidToken(userId);
            UserAccount user = createTestUser();
            user.setId(userId);

            when(passwordResetTokenRepository.findByToken("valid-token-string"))
                    .thenReturn(Optional.of(resetToken));
            when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode(VALID_PASSWORD)).thenReturn("$2a$10$encoded");
            when(userAccountRepository.save(any(UserAccount.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            passwordResetService.resetPassword("valid-token-string", VALID_PASSWORD);

            verify(passwordEncoder).encode(VALID_PASSWORD);
            verify(userAccountRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getPassword()).isEqualTo("$2a$10$encoded");
        }
    }

    @Nested
    @DisplayName("Password Complexity Validation")
    class PasswordComplexityValidation {

        private PasswordResetToken setupValidTokenScenario() {
            UUID userId = UUID.randomUUID();
            PasswordResetToken resetToken = createValidToken(userId);
            when(passwordResetTokenRepository.findByToken("valid-token-string"))
                    .thenReturn(Optional.of(resetToken));
            return resetToken;
        }

        @Test
        @DisplayName("should reject null password")
        void shouldRejectNullPassword() {
            setupValidTokenScenario();

            assertThatThrownBy(() -> passwordResetService.resetPassword("valid-token-string", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Password must be at least 8 characters long");
        }

        @Test
        @DisplayName("should reject password shorter than 8 characters")
        void shouldRejectShortPassword() {
            setupValidTokenScenario();

            assertThatThrownBy(() -> passwordResetService.resetPassword("valid-token-string", "Ab1@xyz"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Password must be at least 8 characters long");
        }

        @Test
        @DisplayName("should reject empty password")
        void shouldRejectEmptyPassword() {
            setupValidTokenScenario();

            assertThatThrownBy(() -> passwordResetService.resetPassword("valid-token-string", ""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Password must be at least 8 characters long");
        }

        @Test
        @DisplayName("should reject password without uppercase letter")
        void shouldRejectPasswordWithoutUppercase() {
            setupValidTokenScenario();

            assertThatThrownBy(() -> passwordResetService.resetPassword("valid-token-string", "lowercase1@pass"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Password must contain at least one uppercase letter");
        }

        @Test
        @DisplayName("should reject password without lowercase letter")
        void shouldRejectPasswordWithoutLowercase() {
            setupValidTokenScenario();

            assertThatThrownBy(() -> passwordResetService.resetPassword("valid-token-string", "UPPERCASE1@PASS"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Password must contain at least one lowercase letter");
        }

        @Test
        @DisplayName("should reject password without digit")
        void shouldRejectPasswordWithoutDigit() {
            setupValidTokenScenario();

            assertThatThrownBy(() -> passwordResetService.resetPassword("valid-token-string", "NoDigit@Pass"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Password must contain at least one digit");
        }

        @Test
        @DisplayName("should reject password without special character")
        void shouldRejectPasswordWithoutSpecialChar() {
            setupValidTokenScenario();

            assertThatThrownBy(() -> passwordResetService.resetPassword("valid-token-string", "NoSpecial1Pass"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Password must contain at least one special character");
        }

        @Test
        @DisplayName("should accept password that meets all complexity requirements")
        void shouldAcceptValidPassword() {
            UUID userId = UUID.randomUUID();
            PasswordResetToken resetToken = createValidToken(userId);
            UserAccount user = createTestUser();
            user.setId(userId);

            when(passwordResetTokenRepository.findByToken("valid-token-string"))
                    .thenReturn(Optional.of(resetToken));
            when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode(VALID_PASSWORD)).thenReturn("encoded");
            when(userAccountRepository.save(any(UserAccount.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Should not throw
            passwordResetService.resetPassword("valid-token-string", VALID_PASSWORD);

            verify(userAccountRepository).save(any());
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "Abcdef1!",       // exactly 8 chars
                "MyP@ssw0rd",     // typical valid password
                "C0mplex!ty",     // special char in middle
                "Test1234$",      // special char at end
                "!1aAAAAA",       // special char at start
        })
        @DisplayName("should accept various valid passwords")
        void shouldAcceptVariousValidPasswords(String password) {
            UUID userId = UUID.randomUUID();
            PasswordResetToken resetToken = createValidToken(userId);
            UserAccount user = createTestUser();
            user.setId(userId);

            when(passwordResetTokenRepository.findByToken("valid-token-string"))
                    .thenReturn(Optional.of(resetToken));
            when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(userAccountRepository.save(any(UserAccount.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            passwordResetService.resetPassword("valid-token-string", password);

            verify(userAccountRepository).save(any());
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "short1!",         // too short
                "nouppercase1!aa", // no uppercase
                "NOLOWERCASE1!",   // no lowercase
                "NoDigitHere!aa",  // no digit
                "NoSpecial1Char",  // no special char
        })
        @DisplayName("should reject various invalid passwords")
        void shouldRejectVariousInvalidPasswords(String password) {
            setupValidTokenScenario();

            assertThatThrownBy(() -> passwordResetService.resetPassword("valid-token-string", password))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("cleanupExpiredTokens")
    class CleanupExpiredTokens {

        @Test
        @DisplayName("should call repository to delete expired and used tokens")
        void shouldCallDeleteOnRepository() {
            when(passwordResetTokenRepository.deleteByExpiresAtBeforeOrUsedTrue(any(Instant.class)))
                    .thenReturn(5);

            passwordResetService.cleanupExpiredTokens();

            verify(passwordResetTokenRepository).deleteByExpiresAtBeforeOrUsedTrue(any(Instant.class));
        }

        @Test
        @DisplayName("should handle case when no tokens to clean up")
        void shouldHandleNoTokensToCleanup() {
            when(passwordResetTokenRepository.deleteByExpiresAtBeforeOrUsedTrue(any(Instant.class)))
                    .thenReturn(0);

            passwordResetService.cleanupExpiredTokens();

            verify(passwordResetTokenRepository).deleteByExpiresAtBeforeOrUsedTrue(any(Instant.class));
        }

        @Test
        @DisplayName("should pass current time as the cutoff point")
        void shouldPassCurrentTimeAsCutoff() {
            ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
            when(passwordResetTokenRepository.deleteByExpiresAtBeforeOrUsedTrue(any(Instant.class)))
                    .thenReturn(0);

            Instant before = Instant.now();
            passwordResetService.cleanupExpiredTokens();
            Instant after = Instant.now();

            verify(passwordResetTokenRepository).deleteByExpiresAtBeforeOrUsedTrue(instantCaptor.capture());
            assertThat(instantCaptor.getValue()).isBetween(before, after);
        }
    }
}
