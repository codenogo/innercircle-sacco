package com.innercircle.sacco.security.service;

import com.innercircle.sacco.common.dto.CursorPage;
import com.innercircle.sacco.security.dto.UserResponse;

import java.util.Set;
import java.util.UUID;

public interface UserManagementService {

    UserResponse activateUser(UUID userId);

    UserResponse deactivateUser(UUID userId);

    UserResponse lockUser(UUID userId);

    UserResponse unlockUser(UUID userId);

    CursorPage<UserResponse> searchUsers(String query, String cursor, int limit);

    UserResponse getUserById(UUID userId);

    CursorPage<UserResponse> listUsers(String cursor, int limit);

    UserResponse updateUserRoles(UUID userId, Set<String> roleNames);

    void deleteUser(UUID userId);
}
