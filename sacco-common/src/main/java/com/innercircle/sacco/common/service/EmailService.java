package com.innercircle.sacco.common.service;

public interface EmailService {

    /**
     * Send password reset email with token
     * @param to recipient email address
     * @param token password reset token
     */
    void sendPasswordResetEmail(String to, String token);

    /**
     * Send welcome email to new user
     * @param to recipient email address
     * @param username username of the new user
     */
    void sendWelcomeEmail(String to, String username);

    /**
     * Send generic email
     * @param to recipient email address
     * @param subject email subject
     * @param body email body
     */
    void sendGenericEmail(String to, String subject, String body);
}
