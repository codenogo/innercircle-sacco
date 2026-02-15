package com.innercircle.sacco.security.controller;

import com.innercircle.sacco.common.dto.ApiResponse;
import com.innercircle.sacco.security.dto.ForgotPasswordRequest;
import com.innercircle.sacco.security.dto.ResetPasswordRequest;
import com.innercircle.sacco.security.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
}
