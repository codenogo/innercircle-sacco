package com.innercircle.sacco.security.controller;

import com.innercircle.sacco.common.dto.ApiResponse;
import com.innercircle.sacco.security.dto.ForgotPasswordRequest;
import com.innercircle.sacco.security.dto.LoginRequest;
import com.innercircle.sacco.security.dto.LoginResponse;
import com.innercircle.sacco.security.dto.RefreshTokenRequest;
import com.innercircle.sacco.security.dto.ResetPasswordRequest;
import com.innercircle.sacco.security.service.AuthService;
import com.innercircle.sacco.security.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final PasswordResetService passwordResetService;
    private final AuthService authService;

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        try {
            passwordResetService.requestPasswordReset(request.getEmail());
            // Always return success to prevent email enumeration
            return ResponseEntity.ok(
                    ApiResponse.ok(null, "If the email exists, a password reset link will be sent")
            );
        } catch (Exception e) {
            log.error("Error processing forgot password request", e);
            // Don't reveal internal errors
            return ResponseEntity.ok(
                    ApiResponse.ok(null, "If the email exists, a password reset link will be sent")
            );
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        try {
            passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
            return ResponseEntity.ok(
                    ApiResponse.ok(null, "Password has been reset successfully")
            );
        } catch (IllegalArgumentException e) {
            log.warn("Password reset failed: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error processing password reset", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An error occurred while resetting the password"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.authenticate(request);
            return ResponseEntity.ok(ApiResponse.ok(response, "Login successful"));
        } catch (BadCredentialsException e) {
            log.warn("Login failed: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid username or password"));
        } catch (DisabledException e) {
            log.warn("Login failed: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Account is disabled"));
        } catch (LockedException e) {
            log.warn("Login failed: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Account is locked"));
        } catch (Exception e) {
            log.error("Error processing login request", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An error occurred while processing the login request"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        try {
            LoginResponse response = authService.refreshAccessToken(request);
            return ResponseEntity.ok(ApiResponse.ok(response, "Token refreshed successfully"));
        } catch (BadCredentialsException e) {
            log.warn("Token refresh failed: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error processing token refresh request", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An error occurred while refreshing the token"));
        }
    }
}
