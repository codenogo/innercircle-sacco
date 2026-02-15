package com.innercircle.sacco.security.controller;

import com.innercircle.sacco.common.dto.ApiResponse;
import com.innercircle.sacco.common.dto.CursorPage;
import com.innercircle.sacco.common.exception.BusinessException;
import com.innercircle.sacco.common.exception.ResourceNotFoundException;
import com.innercircle.sacco.security.dto.UpdateUserRolesRequest;
import com.innercircle.sacco.security.dto.UserResponse;
import com.innercircle.sacco.security.service.UserManagementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserManagementController")
class UserManagementControllerTest {

    @Mock
    private UserManagementService userManagementService;

    @InjectMocks
    private UserManagementController userManagementController;

    private UserResponse createUserResponse(UUID id, String username) {
        return UserResponse.builder()
                .id(id)
                .username(username)
                .email(username + "@example.com")
                .enabled(true)
                .accountNonLocked(true)
                .roles(Set.of("USER"))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/users")
    class ListUsers {

        @Test
        @DisplayName("should return paginated list of users")
        void shouldReturnPaginatedUsers() {
            UUID userId = UUID.randomUUID();
            UserResponse userResponse = createUserResponse(userId, "testuser");
            CursorPage<UserResponse> cursorPage = CursorPage.of(List.of(userResponse), null, false);

            when(userManagementService.listUsers(null, 20)).thenReturn(cursorPage);

            ApiResponse<CursorPage<UserResponse>> response = userManagementController.listUsers(null, 20);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().getItems()).hasSize(1);
            assertThat(response.getData().getItems().get(0).getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("should pass cursor parameter to service")
        void shouldPassCursorToService() {
            String cursor = UUID.randomUUID().toString();
            CursorPage<UserResponse> cursorPage = CursorPage.of(List.of(), null, false);

            when(userManagementService.listUsers(cursor, 10)).thenReturn(cursorPage);

            ApiResponse<CursorPage<UserResponse>> response = userManagementController.listUsers(cursor, 10);

            verify(userManagementService).listUsers(cursor, 10);
            assertThat(response.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("should return hasMore and nextCursor when more results exist")
        void shouldReturnHasMoreInfo() {
            String nextCursor = UUID.randomUUID().toString();
            CursorPage<UserResponse> cursorPage = CursorPage.of(
                    List.of(createUserResponse(UUID.randomUUID(), "user1")),
                    nextCursor, true);

            when(userManagementService.listUsers(null, 1)).thenReturn(cursorPage);

            ApiResponse<CursorPage<UserResponse>> response = userManagementController.listUsers(null, 1);

            assertThat(response.getData().isHasMore()).isTrue();
            assertThat(response.getData().getNextCursor()).isEqualTo(nextCursor);
        }

        @Test
        @DisplayName("should return empty list when no users exist")
        void shouldReturnEmptyList() {
            CursorPage<UserResponse> cursorPage = CursorPage.of(List.of(), null, false);
            when(userManagementService.listUsers(null, 20)).thenReturn(cursorPage);

            ApiResponse<CursorPage<UserResponse>> response = userManagementController.listUsers(null, 20);

            assertThat(response.getData().getItems()).isEmpty();
            assertThat(response.getData().isHasMore()).isFalse();
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/{id}")
    class GetUserById {

        @Test
        @DisplayName("should return user when found")
        void shouldReturnUserWhenFound() {
            UUID userId = UUID.randomUUID();
            UserResponse userResponse = createUserResponse(userId, "testuser");

            when(userManagementService.getUserById(userId)).thenReturn(userResponse);

            ApiResponse<UserResponse> response = userManagementController.getUserById(userId);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().getId()).isEqualTo(userId);
            assertThat(response.getData().getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("should propagate ResourceNotFoundException when user not found")
        void shouldPropagateNotFound() {
            UUID userId = UUID.randomUUID();
            when(userManagementService.getUserById(userId))
                    .thenThrow(new ResourceNotFoundException("User", userId));

            assertThatThrownBy(() -> userManagementController.getUserById(userId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/search")
    class SearchUsers {

        @Test
        @DisplayName("should return matching users for search query")
        void shouldReturnMatchingUsers() {
            String query = "test";
            UserResponse userResponse = createUserResponse(UUID.randomUUID(), "testuser");
            CursorPage<UserResponse> cursorPage = CursorPage.of(List.of(userResponse), null, false);

            when(userManagementService.searchUsers(query, null, 20)).thenReturn(cursorPage);

            ApiResponse<CursorPage<UserResponse>> response =
                    userManagementController.searchUsers(query, null, 20);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().getItems()).hasSize(1);
        }

        @Test
        @DisplayName("should pass all parameters to service")
        void shouldPassAllParameters() {
            String query = "admin";
            String cursor = UUID.randomUUID().toString();
            CursorPage<UserResponse> cursorPage = CursorPage.of(List.of(), null, false);

            when(userManagementService.searchUsers(query, cursor, 5)).thenReturn(cursorPage);

            userManagementController.searchUsers(query, cursor, 5);

            verify(userManagementService).searchUsers(query, cursor, 5);
        }

        @Test
        @DisplayName("should return empty result when no matches found")
        void shouldReturnEmptyForNoMatches() {
            CursorPage<UserResponse> cursorPage = CursorPage.of(List.of(), null, false);
            when(userManagementService.searchUsers("nomatch", null, 20)).thenReturn(cursorPage);

            ApiResponse<CursorPage<UserResponse>> response =
                    userManagementController.searchUsers("nomatch", null, 20);

            assertThat(response.getData().getItems()).isEmpty();
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/users/{id}/activate")
    class ActivateUser {

        @Test
        @DisplayName("should activate user and return success message")
        void shouldActivateUser() {
            UUID userId = UUID.randomUUID();
            UserResponse userResponse = createUserResponse(userId, "testuser");

            when(userManagementService.activateUser(userId)).thenReturn(userResponse);

            ApiResponse<UserResponse> response = userManagementController.activateUser(userId);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getMessage()).isEqualTo("User activated successfully");
            assertThat(response.getData().getId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("should propagate ResourceNotFoundException")
        void shouldPropagateNotFound() {
            UUID userId = UUID.randomUUID();
            when(userManagementService.activateUser(userId))
                    .thenThrow(new ResourceNotFoundException("User", userId));

            assertThatThrownBy(() -> userManagementController.activateUser(userId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/users/{id}/deactivate")
    class DeactivateUser {

        @Test
        @DisplayName("should deactivate user and return success message")
        void shouldDeactivateUser() {
            UUID userId = UUID.randomUUID();
            UserResponse userResponse = createUserResponse(userId, "testuser");
            userResponse.setEnabled(false);

            when(userManagementService.deactivateUser(userId)).thenReturn(userResponse);

            ApiResponse<UserResponse> response = userManagementController.deactivateUser(userId);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getMessage()).isEqualTo("User deactivated successfully");
            assertThat(response.getData().getEnabled()).isFalse();
        }

        @Test
        @DisplayName("should propagate BusinessException for self-deactivation")
        void shouldPropagateSelfDeactivation() {
            UUID userId = UUID.randomUUID();
            when(userManagementService.deactivateUser(userId))
                    .thenThrow(new BusinessException("Cannot deactivate your own account"));

            assertThatThrownBy(() -> userManagementController.deactivateUser(userId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Cannot deactivate your own account");
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/users/{id}/lock")
    class LockUser {

        @Test
        @DisplayName("should lock user and return success message")
        void shouldLockUser() {
            UUID userId = UUID.randomUUID();
            UserResponse userResponse = createUserResponse(userId, "testuser");
            userResponse.setAccountNonLocked(false);

            when(userManagementService.lockUser(userId)).thenReturn(userResponse);

            ApiResponse<UserResponse> response = userManagementController.lockUser(userId);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getMessage()).isEqualTo("User account locked successfully");
            assertThat(response.getData().getAccountNonLocked()).isFalse();
        }

        @Test
        @DisplayName("should propagate BusinessException for self-locking")
        void shouldPropagateSelfLocking() {
            UUID userId = UUID.randomUUID();
            when(userManagementService.lockUser(userId))
                    .thenThrow(new BusinessException("Cannot lock your own account"));

            assertThatThrownBy(() -> userManagementController.lockUser(userId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Cannot lock your own account");
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/users/{id}/unlock")
    class UnlockUser {

        @Test
        @DisplayName("should unlock user and return success message")
        void shouldUnlockUser() {
            UUID userId = UUID.randomUUID();
            UserResponse userResponse = createUserResponse(userId, "testuser");

            when(userManagementService.unlockUser(userId)).thenReturn(userResponse);

            ApiResponse<UserResponse> response = userManagementController.unlockUser(userId);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getMessage()).isEqualTo("User account unlocked successfully");
            assertThat(response.getData().getAccountNonLocked()).isTrue();
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/users/{id}/roles")
    class UpdateUserRoles {

        @Test
        @DisplayName("should update user roles and return success message")
        void shouldUpdateUserRoles() {
            UUID userId = UUID.randomUUID();
            Set<String> roleNames = Set.of("ADMIN", "USER");
            UpdateUserRolesRequest request = UpdateUserRolesRequest.builder()
                    .roleNames(roleNames)
                    .build();

            UserResponse userResponse = createUserResponse(userId, "testuser");
            userResponse.setRoles(roleNames);

            when(userManagementService.updateUserRoles(userId, roleNames)).thenReturn(userResponse);

            ApiResponse<UserResponse> response = userManagementController.updateUserRoles(userId, request);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getMessage()).isEqualTo("User roles updated successfully");
            assertThat(response.getData().getRoles()).containsExactlyInAnyOrder("ADMIN", "USER");
        }

        @Test
        @DisplayName("should propagate BusinessException for self-admin-role-removal")
        void shouldPropagateSelfAdminRoleRemoval() {
            UUID userId = UUID.randomUUID();
            Set<String> roleNames = Set.of("USER");
            UpdateUserRolesRequest request = UpdateUserRolesRequest.builder()
                    .roleNames(roleNames)
                    .build();

            when(userManagementService.updateUserRoles(userId, roleNames))
                    .thenThrow(new BusinessException("Cannot remove ADMIN role from your own account"));

            assertThatThrownBy(() -> userManagementController.updateUserRoles(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Cannot remove ADMIN role from your own account");
        }

        @Test
        @DisplayName("should propagate ResourceNotFoundException for non-existent role")
        void shouldPropagateRoleNotFound() {
            UUID userId = UUID.randomUUID();
            Set<String> roleNames = Set.of("NONEXISTENT");
            UpdateUserRolesRequest request = UpdateUserRolesRequest.builder()
                    .roleNames(roleNames)
                    .build();

            when(userManagementService.updateUserRoles(userId, roleNames))
                    .thenThrow(new ResourceNotFoundException("Role", "NONEXISTENT"));

            assertThatThrownBy(() -> userManagementController.updateUserRoles(userId, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should propagate BusinessException for empty role set")
        void shouldPropagateEmptyRoleSet() {
            UUID userId = UUID.randomUUID();
            Set<String> roleNames = Set.of("ADMIN");
            UpdateUserRolesRequest request = UpdateUserRolesRequest.builder()
                    .roleNames(roleNames)
                    .build();

            when(userManagementService.updateUserRoles(userId, roleNames))
                    .thenThrow(new BusinessException("At least one role must be specified"));

            assertThatThrownBy(() -> userManagementController.updateUserRoles(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("At least one role must be specified");
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/users/{id}")
    class DeleteUser {

        @Test
        @DisplayName("should delete user and return success message")
        void shouldDeleteUser() {
            UUID userId = UUID.randomUUID();
            doNothing().when(userManagementService).deleteUser(userId);

            ApiResponse<Void> response = userManagementController.deleteUser(userId);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getMessage()).isEqualTo("User deleted successfully");
            assertThat(response.getData()).isNull();
            verify(userManagementService).deleteUser(userId);
        }

        @Test
        @DisplayName("should propagate BusinessException for self-deletion")
        void shouldPropagateSelfDeletion() {
            UUID userId = UUID.randomUUID();
            doThrow(new BusinessException("Cannot delete your own account"))
                    .when(userManagementService).deleteUser(userId);

            assertThatThrownBy(() -> userManagementController.deleteUser(userId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Cannot delete your own account");
        }

        @Test
        @DisplayName("should propagate ResourceNotFoundException when user not found")
        void shouldPropagateNotFound() {
            UUID userId = UUID.randomUUID();
            doThrow(new ResourceNotFoundException("User", userId))
                    .when(userManagementService).deleteUser(userId);

            assertThatThrownBy(() -> userManagementController.deleteUser(userId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
