package com.innercircle.sacco.security.controller;

import com.innercircle.sacco.common.dto.ApiResponse;
import com.innercircle.sacco.common.dto.CursorPage;
import com.innercircle.sacco.security.dto.UpdateUserRolesRequest;
import com.innercircle.sacco.security.dto.UserResponse;
import com.innercircle.sacco.security.service.UserManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserManagementController {

    private final UserManagementService userManagementService;

    @GetMapping
    public ApiResponse<CursorPage<UserResponse>> listUsers(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size) {

        CursorPage<UserResponse> page = userManagementService.listUsers(cursor, size);
        return ApiResponse.ok(page);
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getUserById(@PathVariable UUID id) {
        UserResponse user = userManagementService.getUserById(id);
        return ApiResponse.ok(user);
    }

    @GetMapping("/search")
    public ApiResponse<CursorPage<UserResponse>> searchUsers(
            @RequestParam String q,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size) {

        CursorPage<UserResponse> page = userManagementService.searchUsers(q, cursor, size);
        return ApiResponse.ok(page);
    }

    @PatchMapping("/{id}/activate")
    public ApiResponse<UserResponse> activateUser(@PathVariable UUID id) {
        UserResponse user = userManagementService.activateUser(id);
        return ApiResponse.ok(user, "User activated successfully");
    }

    @PatchMapping("/{id}/deactivate")
    public ApiResponse<UserResponse> deactivateUser(@PathVariable UUID id) {
        UserResponse user = userManagementService.deactivateUser(id);
        return ApiResponse.ok(user, "User deactivated successfully");
    }

    @PatchMapping("/{id}/lock")
    public ApiResponse<UserResponse> lockUser(@PathVariable UUID id) {
        UserResponse user = userManagementService.lockUser(id);
        return ApiResponse.ok(user, "User account locked successfully");
    }

    @PatchMapping("/{id}/unlock")
    public ApiResponse<UserResponse> unlockUser(@PathVariable UUID id) {
        UserResponse user = userManagementService.unlockUser(id);
        return ApiResponse.ok(user, "User account unlocked successfully");
    }

    @PutMapping("/{id}/roles")
    public ApiResponse<UserResponse> updateUserRoles(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRolesRequest request) {

        UserResponse user = userManagementService.updateUserRoles(id, request.getRoleNames());
        return ApiResponse.ok(user, "User roles updated successfully");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable UUID id) {
        userManagementService.deleteUser(id);
        return ApiResponse.ok(null, "User deleted successfully");
    }
}
