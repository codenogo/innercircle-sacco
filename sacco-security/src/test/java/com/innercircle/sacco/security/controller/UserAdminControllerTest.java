package com.innercircle.sacco.security.controller;

import com.innercircle.sacco.common.dto.ApiResponse;
import com.innercircle.sacco.common.exception.ResourceNotFoundException;
import com.innercircle.sacco.security.dto.CreateUserRequest;
import com.innercircle.sacco.security.dto.UserResponse;
import com.innercircle.sacco.security.entity.UserAccount;
import com.innercircle.sacco.security.repository.UserAccountRepository;
import com.innercircle.sacco.security.service.PasswordResetService;
import com.innercircle.sacco.security.service.UserManagementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserAdminController")
class UserAdminControllerTest {

    @Mock
    private UserManagementService userManagementService;

    @Mock
    private PasswordResetService passwordResetService;

    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private UserAdminController userAdminController;

    @Nested
    @DisplayName("createUser")
    class CreateUser {

        @Test
        @DisplayName("should return 201 with user details")
        void shouldReturn201WithUserDetails() {
            UUID userId = UUID.randomUUID();
            CreateUserRequest request = CreateUserRequest.builder()
                    .username("newuser")
                    .email("newuser@example.com")
                    .roleNames(Set.of("MEMBER"))
                    .sendPasswordResetEmail(false)
                    .build();

            UserResponse expectedResponse = UserResponse.builder()
                    .id(userId)
                    .username("newuser")
                    .email("newuser@example.com")
                    .enabled(true)
                    .accountNonLocked(true)
                    .roles(Set.of("MEMBER"))
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(userManagementService.createUser(request)).thenReturn(expectedResponse);

            ResponseEntity<ApiResponse<UserResponse>> response = userAdminController.createUser(request);

            assertThat(response.getStatusCode().value()).isEqualTo(201);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData().getUsername()).isEqualTo("newuser");
            assertThat(response.getBody().getData().getEmail()).isEqualTo("newuser@example.com");
            assertThat(response.getBody().getData().getRoles()).containsExactly("MEMBER");
            verify(userManagementService).createUser(request);
        }
    }

    @Nested
    @DisplayName("triggerPasswordReset")
    class TriggerPasswordReset {

        @Test
        @DisplayName("should call PasswordResetService with user email")
        void shouldCallPasswordResetService() {
            UUID userId = UUID.randomUUID();
            UserAccount user = new UserAccount();
            user.setId(userId);
            user.setUsername("testuser");
            user.setEmail("testuser@example.com");

            when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));

            ResponseEntity<ApiResponse<Void>> response = userAdminController.triggerPasswordReset(userId);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            verify(passwordResetService).requestPasswordReset("testuser@example.com");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user is not found")
        void shouldThrowWhenUserNotFound() {
            UUID unknownId = UUID.randomUUID();
            when(userAccountRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userAdminController.triggerPasswordReset(unknownId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
