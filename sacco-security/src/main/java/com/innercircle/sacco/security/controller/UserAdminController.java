package com.innercircle.sacco.security.controller;

import com.innercircle.sacco.common.dto.ApiResponse;
import com.innercircle.sacco.common.exception.ResourceNotFoundException;
import com.innercircle.sacco.security.dto.CreateUserRequest;
import com.innercircle.sacco.security.dto.UserResponse;
import com.innercircle.sacco.security.entity.UserAccount;
import com.innercircle.sacco.security.repository.UserAccountRepository;
import com.innercircle.sacco.security.service.PasswordResetService;
import com.innercircle.sacco.security.service.UserManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserManagementService userManagementService;
    private final PasswordResetService passwordResetService;
    private final UserAccountRepository userAccountRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        UserResponse userResponse = userManagementService.createUser(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(userResponse, "User created successfully"));
    }

    @PostMapping("/{id}/password-reset")
    public ResponseEntity<ApiResponse<Void>> triggerPasswordReset(@PathVariable UUID id) {
        UserAccount user = userAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        passwordResetService.requestPasswordReset(user.getEmail());

        return ResponseEntity.ok(
                ApiResponse.ok(null, "Password reset email sent")
        );
    }
}
