package com.innercircle.sacco.security.controller;

import com.innercircle.sacco.common.dto.ApiResponse;
import com.innercircle.sacco.security.dto.ForgotPasswordRequest;
import com.innercircle.sacco.security.dto.ResetPasswordRequest;
import com.innercircle.sacco.security.service.PasswordResetService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController")
class AuthControllerTest {

    @Mock
    private PasswordResetService passwordResetService;

    @InjectMocks
    private AuthController authController;

    @Nested
    @DisplayName("POST /api/auth/forgot-password")
    class ForgotPassword {

        @Test
        @DisplayName("should return 200 OK with generic message on success")
        void shouldReturnOkOnSuccess() {
            ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                    .email("user@example.com")
                    .build();
            doNothing().when(passwordResetService).requestPasswordReset("user@example.com");

            ResponseEntity<ApiResponse<Void>> response = authController.forgotPassword(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getMessage())
                    .isEqualTo("If the email exists, a password reset link will be sent");
            verify(passwordResetService).requestPasswordReset("user@example.com");
        }

        @Test
        @DisplayName("should return 200 OK with same generic message when service throws exception")
        void shouldReturnOkEvenOnException() {
            ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                    .email("user@example.com")
                    .build();
            doThrow(new RuntimeException("Internal error"))
                    .when(passwordResetService).requestPasswordReset("user@example.com");

            ResponseEntity<ApiResponse<Void>> response = authController.forgotPassword(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getMessage())
                    .isEqualTo("If the email exists, a password reset link will be sent");
        }

        @Test
        @DisplayName("should not reveal email existence in response")
        void shouldNotRevealEmailExistence() {
            ForgotPasswordRequest existingEmail = ForgotPasswordRequest.builder()
                    .email("exists@example.com")
                    .build();
            ForgotPasswordRequest nonExistingEmail = ForgotPasswordRequest.builder()
                    .email("notexists@example.com")
                    .build();

            doNothing().when(passwordResetService).requestPasswordReset("exists@example.com");
            doNothing().when(passwordResetService).requestPasswordReset("notexists@example.com");

            ResponseEntity<ApiResponse<Void>> response1 = authController.forgotPassword(existingEmail);
            ResponseEntity<ApiResponse<Void>> response2 = authController.forgotPassword(nonExistingEmail);

            assertThat(response1.getBody().getMessage())
                    .isEqualTo(response2.getBody().getMessage());
            assertThat(response1.getStatusCode()).isEqualTo(response2.getStatusCode());
        }

        @Test
        @DisplayName("should return null data in response body")
        void shouldReturnNullData() {
            ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                    .email("user@example.com")
                    .build();
            doNothing().when(passwordResetService).requestPasswordReset("user@example.com");

            ResponseEntity<ApiResponse<Void>> response = authController.forgotPassword(request);

            assertThat(response.getBody().getData()).isNull();
        }
    }

    @Nested
    @DisplayName("POST /api/auth/reset-password")
    class ResetPassword {

        @Test
        @DisplayName("should return 200 OK on successful password reset")
        void shouldReturnOkOnSuccess() {
            ResetPasswordRequest request = ResetPasswordRequest.builder()
                    .token("valid-token")
                    .newPassword("StrongP@ss1")
                    .build();
            doNothing().when(passwordResetService).resetPassword("valid-token", "StrongP@ss1");

            ResponseEntity<ApiResponse<Void>> response = authController.resetPassword(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getMessage())
                    .isEqualTo("Password has been reset successfully");
            verify(passwordResetService).resetPassword("valid-token", "StrongP@ss1");
        }

        @Test
        @DisplayName("should return 400 BAD REQUEST when IllegalArgumentException is thrown")
        void shouldReturnBadRequestOnIllegalArgument() {
            ResetPasswordRequest request = ResetPasswordRequest.builder()
                    .token("invalid-token")
                    .newPassword("StrongP@ss1")
                    .build();
            doThrow(new IllegalArgumentException("Invalid or expired token"))
                    .when(passwordResetService).resetPassword("invalid-token", "StrongP@ss1");

            ResponseEntity<ApiResponse<Void>> response = authController.resetPassword(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isFalse();
            assertThat(response.getBody().getMessage()).isEqualTo("Invalid or expired token");
        }

        @Test
        @DisplayName("should return 400 BAD REQUEST for expired token error")
        void shouldReturnBadRequestForExpiredToken() {
            ResetPasswordRequest request = ResetPasswordRequest.builder()
                    .token("expired-token")
                    .newPassword("StrongP@ss1")
                    .build();
            doThrow(new IllegalArgumentException("Token has expired"))
                    .when(passwordResetService).resetPassword("expired-token", "StrongP@ss1");

            ResponseEntity<ApiResponse<Void>> response = authController.resetPassword(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().getMessage()).isEqualTo("Token has expired");
        }

        @Test
        @DisplayName("should return 400 BAD REQUEST for used token error")
        void shouldReturnBadRequestForUsedToken() {
            ResetPasswordRequest request = ResetPasswordRequest.builder()
                    .token("used-token")
                    .newPassword("StrongP@ss1")
                    .build();
            doThrow(new IllegalArgumentException("Token has already been used"))
                    .when(passwordResetService).resetPassword("used-token", "StrongP@ss1");

            ResponseEntity<ApiResponse<Void>> response = authController.resetPassword(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().getMessage()).isEqualTo("Token has already been used");
        }

        @Test
        @DisplayName("should return 400 BAD REQUEST for password complexity error")
        void shouldReturnBadRequestForWeakPassword() {
            ResetPasswordRequest request = ResetPasswordRequest.builder()
                    .token("valid-token")
                    .newPassword("weak")
                    .build();
            doThrow(new IllegalArgumentException("Password must be at least 8 characters long"))
                    .when(passwordResetService).resetPassword("valid-token", "weak");

            ResponseEntity<ApiResponse<Void>> response = authController.resetPassword(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().getMessage())
                    .isEqualTo("Password must be at least 8 characters long");
        }

        @Test
        @DisplayName("should return 500 INTERNAL SERVER ERROR for unexpected exceptions")
        void shouldReturnInternalServerErrorOnUnexpectedException() {
            ResetPasswordRequest request = ResetPasswordRequest.builder()
                    .token("valid-token")
                    .newPassword("StrongP@ss1")
                    .build();
            doThrow(new RuntimeException("Database connection failed"))
                    .when(passwordResetService).resetPassword("valid-token", "StrongP@ss1");

            ResponseEntity<ApiResponse<Void>> response = authController.resetPassword(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isFalse();
            assertThat(response.getBody().getMessage())
                    .isEqualTo("An error occurred while resetting the password");
        }

        @Test
        @DisplayName("should not expose internal error details in 500 response")
        void shouldNotExposeInternalErrorDetails() {
            ResetPasswordRequest request = ResetPasswordRequest.builder()
                    .token("valid-token")
                    .newPassword("StrongP@ss1")
                    .build();
            doThrow(new RuntimeException("Sensitive database info: SQL injection detected"))
                    .when(passwordResetService).resetPassword("valid-token", "StrongP@ss1");

            ResponseEntity<ApiResponse<Void>> response = authController.resetPassword(request);

            assertThat(response.getBody().getMessage())
                    .doesNotContain("SQL")
                    .doesNotContain("database")
                    .doesNotContain("Sensitive");
        }

        @Test
        @DisplayName("should return null data in successful response body")
        void shouldReturnNullData() {
            ResetPasswordRequest request = ResetPasswordRequest.builder()
                    .token("valid-token")
                    .newPassword("StrongP@ss1")
                    .build();
            doNothing().when(passwordResetService).resetPassword("valid-token", "StrongP@ss1");

            ResponseEntity<ApiResponse<Void>> response = authController.resetPassword(request);

            assertThat(response.getBody().getData()).isNull();
        }
    }
}
