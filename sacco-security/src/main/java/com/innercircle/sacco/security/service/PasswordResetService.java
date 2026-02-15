package com.innercircle.sacco.security.service;

public interface PasswordResetService {

    /**
     * Request password reset for a user
     * @param email email address of the user
     */
    void requestPasswordReset(String email);

    /**
     * Reset user password with token
     * @param token password reset token
     * @param newPassword new password
     */
    void resetPassword(String token, String newPassword);

    /**
     * Clean up expired and used password reset tokens
     */
    void cleanupExpiredTokens();
}
