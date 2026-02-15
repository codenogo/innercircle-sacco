package com.innercircle.sacco.security.service;

import com.innercircle.sacco.common.dto.CursorPage;
import com.innercircle.sacco.common.exception.BusinessException;
import com.innercircle.sacco.common.exception.ResourceNotFoundException;
import com.innercircle.sacco.security.dto.UserResponse;
import com.innercircle.sacco.security.entity.Role;
import com.innercircle.sacco.security.entity.UserAccount;
import com.innercircle.sacco.security.repository.RoleRepository;
import com.innercircle.sacco.security.repository.UserAccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserManagementServiceImpl")
class UserManagementServiceImplTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private UserManagementServiceImpl userManagementService;

    private static final String ADMIN_USERNAME = "admin";
    private static final String OTHER_USERNAME = "otheruser";

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setupSecurityContext(String username) {
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(username);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private void setupNullSecurityContext() {
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(securityContext);
    }

    private UserAccount createTestUser(UUID id, String username, String email) {
        Role userRole = new Role();
        userRole.setId(UUID.randomUUID());
        userRole.setName("USER");

        UserAccount user = new UserAccount();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("encodedPassword");
        user.setEnabled(true);
        user.setAccountNonLocked(true);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        user.setRoles(new HashSet<>(Set.of(userRole)));
        return user;
    }

    private UserAccount createAdminUser(UUID id) {
        Role adminRole = new Role();
        adminRole.setId(UUID.randomUUID());
        adminRole.setName("ADMIN");

        UserAccount user = createTestUser(id, ADMIN_USERNAME, "admin@example.com");
        user.getRoles().add(adminRole);
        return user;
    }

    @Nested
    @DisplayName("activateUser")
    class ActivateUser {

        @Test
        @DisplayName("should activate a disabled user")
        void shouldActivateDisabledUser() {
            UUID userId = UUID.randomUUID();
            UserAccount user = createTestUser(userId, OTHER_USERNAME, "other@example.com");
            user.setEnabled(false);

            when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userAccountRepository.save(any(UserAccount.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            UserResponse response = userManagementService.activateUser(userId);

            assertThat(response.getEnabled()).isTrue();
            assertThat(response.getUsername()).isEqualTo(OTHER_USERNAME);
            verify(userAccountRepository).save(user);
        }

        @Test
        @DisplayName("should activate an already enabled user without error")
        void shouldActivateAlreadyEnabledUser() {
            UUID userId = UUID.randomUUID();
            UserAccount user = createTestUser(userId, OTHER_USERNAME, "other@example.com");

            when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userAccountRepository.save(any(UserAccount.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            UserResponse response = userManagementService.activateUser(userId);

            assertThat(response.getEnabled()).isTrue();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user not found")
        void shouldThrowWhenUserNotFound() {
            UUID userId = UUID.randomUUID();
            when(userAccountRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userManagementService.activateUser(userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User")
                    .hasMessageContaining(userId.toString());
        }

        @Test
        @DisplayName("should return correct UserResponse fields")
        void shouldReturnCorrectUserResponse() {
            UUID userId = UUID.randomUUID();
            UserAccount user = createTestUser(userId, OTHER_USERNAME, "other@example.com");

            when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userAccountRepository.save(any(UserAccount.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            UserResponse response = userManagementService.activateUser(userId);

            assertThat(response.getId()).isEqualTo(userId);
            assertThat(response.getUsername()).isEqualTo(OTHER_USERNAME);
            assertThat(response.getEmail()).isEqualTo("other@example.com");
            assertThat(response.getEnabled()).isTrue();
            assertThat(response.getAccountNonLocked()).isTrue();
            assertThat(response.getRoles()).contains("USER");
            assertThat(response.getCreatedAt()).isNotNull();
            assertThat(response.getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("deactivateUser")
    class DeactivateUser {

        @Test
        @DisplayName("should deactivate an enabled user")
        void shouldDeactivateEnabledUser() {
            setupSecurityContext(ADMIN_USERNAME);
            UUID adminId = UUID.randomUUID();
            UUID targetId = UUID.randomUUID();

            UserAccount adminUser = createAdminUser(adminId);
            UserAccount targetUser = createTestUser(targetId, OTHER_USERNAME, "other@example.com");

            when(userAccountRepository.findByUsername(ADMIN_USERNAME)).thenReturn(Optional.of(adminUser));
            when(userAccountRepository.findById(targetId)).thenReturn(Optional.of(targetUser));
            when(userAccountRepository.save(any(UserAccount.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            UserResponse response = userManagementService.deactivateUser(targetId);

            assertThat(response.getEnabled()).isFalse();
        }

        @Test
        @DisplayName("should prevent self-deactivation")
        void shouldPreventSelfDeactivation() {
            setupSecurityContext(ADMIN_USERNAME);
            UUID adminId = UUID.randomUUID();
            UserAccount adminUser = createAdminUser(adminId);

            when(userAccountRepository.findByUsername(ADMIN_USERNAME)).thenReturn(Optional.of(adminUser));

            assertThatThrownBy(() -> userManagementService.deactivateUser(adminId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Cannot deactivate your own account");
        }

        @Test
        @DisplayName("should allow deactivation when no authentication context")
        void shouldAllowDeactivationWithNoAuth() {
            setupNullSecurityContext();
            UUID targetId = UUID.randomUUID();
            UserAccount targetUser = createTestUser(targetId, OTHER_USERNAME, "other@example.com");

            when(userAccountRepository.findById(targetId)).thenReturn(Optional.of(targetUser));
            when(userAccountRepository.save(any(UserAccount.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            UserResponse response = userManagementService.deactivateUser(targetId);

            assertThat(response.getEnabled()).isFalse();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when target user not found")
        void shouldThrowWhenTargetNotFound() {
            setupSecurityContext(ADMIN_USERNAME);
            UUID adminId = UUID.randomUUID();
            UUID targetId = UUID.randomUUID();
            UserAccount adminUser = createAdminUser(adminId);

            when(userAccountRepository.findByUsername(ADMIN_USERNAME)).thenReturn(Optional.of(adminUser));
            when(userAccountRepository.findById(targetId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userManagementService.deactivateUser(targetId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("lockUser")
    class LockUser {

        @Test
        @DisplayName("should lock an unlocked user")
        void shouldLockUser() {
            setupSecurityContext(ADMIN_USERNAME);
            UUID adminId = UUID.randomUUID();
            UUID targetId = UUID.randomUUID();

            UserAccount adminUser = createAdminUser(adminId);
            UserAccount targetUser = createTestUser(targetId, OTHER_USERNAME, "other@example.com");

            when(userAccountRepository.findByUsername(ADMIN_USERNAME)).thenReturn(Optional.of(adminUser));
            when(userAccountRepository.findById(targetId)).thenReturn(Optional.of(targetUser));
            when(userAccountRepository.save(any(UserAccount.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            UserResponse response = userManagementService.lockUser(targetId);

            assertThat(response.getAccountNonLocked()).isFalse();
        }

        @Test
        @DisplayName("should prevent self-locking")
        void shouldPreventSelfLocking() {
            setupSecurityContext(ADMIN_USERNAME);
            UUID adminId = UUID.randomUUID();
            UserAccount adminUser = createAdminUser(adminId);

            when(userAccountRepository.findByUsername(ADMIN_USERNAME)).thenReturn(Optional.of(adminUser));

            assertThatThrownBy(() -> userManagementService.lockUser(adminId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Cannot lock your own account");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user not found")
        void shouldThrowWhenUserNotFound() {
            setupSecurityContext(ADMIN_USERNAME);
            UUID adminId = UUID.randomUUID();
            UUID targetId = UUID.randomUUID();
            UserAccount adminUser = createAdminUser(adminId);

            when(userAccountRepository.findByUsername(ADMIN_USERNAME)).thenReturn(Optional.of(adminUser));
            when(userAccountRepository.findById(targetId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userManagementService.lockUser(targetId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("unlockUser")
    class UnlockUser {

        @Test
        @DisplayName("should unlock a locked user")
        void shouldUnlockUser() {
            UUID targetId = UUID.randomUUID();
            UserAccount targetUser = createTestUser(targetId, OTHER_USERNAME, "other@example.com");
            targetUser.setAccountNonLocked(false);

            when(userAccountRepository.findById(targetId)).thenReturn(Optional.of(targetUser));
            when(userAccountRepository.save(any(UserAccount.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            UserResponse response = userManagementService.unlockUser(targetId);

            assertThat(response.getAccountNonLocked()).isTrue();
        }

        @Test
        @DisplayName("should unlock an already unlocked user without error")
        void shouldUnlockAlreadyUnlockedUser() {
            UUID targetId = UUID.randomUUID();
            UserAccount targetUser = createTestUser(targetId, OTHER_USERNAME, "other@example.com");

            when(userAccountRepository.findById(targetId)).thenReturn(Optional.of(targetUser));
            when(userAccountRepository.save(any(UserAccount.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            UserResponse response = userManagementService.unlockUser(targetId);

            assertThat(response.getAccountNonLocked()).isTrue();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user not found")
        void shouldThrowWhenUserNotFound() {
            UUID targetId = UUID.randomUUID();
            when(userAccountRepository.findById(targetId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userManagementService.unlockUser(targetId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getUserById")
    class GetUserById {

        @Test
        @DisplayName("should return user response when user exists")
        void shouldReturnUserWhenExists() {
            UUID userId = UUID.randomUUID();
            UserAccount user = createTestUser(userId, OTHER_USERNAME, "other@example.com");

            when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));

            UserResponse response = userManagementService.getUserById(userId);

            assertThat(response.getId()).isEqualTo(userId);
            assertThat(response.getUsername()).isEqualTo(OTHER_USERNAME);
            assertThat(response.getEmail()).isEqualTo("other@example.com");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user not found")
        void shouldThrowWhenNotFound() {
            UUID userId = UUID.randomUUID();
            when(userAccountRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userManagementService.getUserById(userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User");
        }

        @Test
        @DisplayName("should correctly map roles in response")
        void shouldMapRolesCorrectly() {
            UUID userId = UUID.randomUUID();
            UserAccount user = createAdminUser(userId);

            when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));

            UserResponse response = userManagementService.getUserById(userId);

            assertThat(response.getRoles()).contains("ADMIN", "USER");
        }
    }

    @Nested
    @DisplayName("listUsers")
    class ListUsers {

        @Test
        @DisplayName("should return first page when cursor is null")
        void shouldReturnFirstPageWhenNoCursor() {
            List<UserAccount> users = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                users.add(createTestUser(UUID.randomUUID(), "user" + i, "user" + i + "@example.com"));
            }
            Page<UserAccount> page = new PageImpl<>(users);

            when(userAccountRepository.findAll(any(PageRequest.class))).thenReturn(page);

            CursorPage<UserResponse> result = userManagementService.listUsers(null, 10);

            assertThat(result.getItems()).hasSize(3);
            assertThat(result.isHasMore()).isFalse();
            assertThat(result.getNextCursor()).isNull();
        }

        @Test
        @DisplayName("should return first page when cursor is empty string")
        void shouldReturnFirstPageWhenEmptyCursor() {
            List<UserAccount> users = new ArrayList<>();
            users.add(createTestUser(UUID.randomUUID(), "user0", "user0@example.com"));
            Page<UserAccount> page = new PageImpl<>(users);

            when(userAccountRepository.findAll(any(PageRequest.class))).thenReturn(page);

            CursorPage<UserResponse> result = userManagementService.listUsers("", 10);

            assertThat(result.getItems()).hasSize(1);
        }

        @Test
        @DisplayName("should use cursor for pagination when cursor is provided")
        void shouldUseCursorForPagination() {
            UUID cursorId = UUID.randomUUID();
            List<UserAccount> users = new ArrayList<>();
            users.add(createTestUser(UUID.randomUUID(), "user1", "user1@example.com"));

            when(userAccountRepository.findByIdGreaterThanOrderById(eq(cursorId), any(PageRequest.class)))
                    .thenReturn(users);

            CursorPage<UserResponse> result = userManagementService.listUsers(cursorId.toString(), 10);

            assertThat(result.getItems()).hasSize(1);
            verify(userAccountRepository).findByIdGreaterThanOrderById(eq(cursorId), any(PageRequest.class));
        }

        @Test
        @DisplayName("should indicate hasMore when results exceed limit")
        void shouldIndicateHasMore() {
            int limit = 2;
            List<UserAccount> users = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                users.add(createTestUser(UUID.randomUUID(), "user" + i, "user" + i + "@example.com"));
            }
            Page<UserAccount> page = new PageImpl<>(users);

            when(userAccountRepository.findAll(any(PageRequest.class))).thenReturn(page);

            CursorPage<UserResponse> result = userManagementService.listUsers(null, limit);

            assertThat(result.isHasMore()).isTrue();
            assertThat(result.getItems()).hasSize(limit);
            assertThat(result.getNextCursor()).isNotNull();
        }

        @Test
        @DisplayName("should set nextCursor to last item's ID when hasMore is true")
        void shouldSetNextCursorCorrectly() {
            int limit = 2;
            UUID lastId = UUID.randomUUID();
            List<UserAccount> users = new ArrayList<>();
            users.add(createTestUser(UUID.randomUUID(), "user0", "user0@example.com"));
            users.add(createTestUser(lastId, "user1", "user1@example.com"));
            users.add(createTestUser(UUID.randomUUID(), "user2", "user2@example.com"));

            Page<UserAccount> page = new PageImpl<>(users);
            when(userAccountRepository.findAll(any(PageRequest.class))).thenReturn(page);

            CursorPage<UserResponse> result = userManagementService.listUsers(null, limit);

            assertThat(result.getNextCursor()).isEqualTo(lastId.toString());
        }

        @Test
        @DisplayName("should return empty list when no users exist")
        void shouldReturnEmptyList() {
            Page<UserAccount> page = new PageImpl<>(List.of());
            when(userAccountRepository.findAll(any(PageRequest.class))).thenReturn(page);

            CursorPage<UserResponse> result = userManagementService.listUsers(null, 10);

            assertThat(result.getItems()).isEmpty();
            assertThat(result.isHasMore()).isFalse();
            assertThat(result.getNextCursor()).isNull();
        }

        @Test
        @DisplayName("should request limit + 1 records for has-more detection")
        void shouldRequestLimitPlusOne() {
            Page<UserAccount> page = new PageImpl<>(List.of());
            when(userAccountRepository.findAll(any(PageRequest.class))).thenReturn(page);

            userManagementService.listUsers(null, 5);

            verify(userAccountRepository).findAll(PageRequest.of(0, 6));
        }
    }

    @Nested
    @DisplayName("searchUsers")
    class SearchUsers {

        @Test
        @DisplayName("should delegate to listUsers when query is null")
        void shouldDelegateToListUsersWhenQueryNull() {
            Page<UserAccount> page = new PageImpl<>(List.of());
            when(userAccountRepository.findAll(any(PageRequest.class))).thenReturn(page);

            CursorPage<UserResponse> result = userManagementService.searchUsers(null, null, 10);

            verify(userAccountRepository).findAll(any(PageRequest.class));
            assertThat(result.getItems()).isEmpty();
        }

        @Test
        @DisplayName("should delegate to listUsers when query is blank")
        void shouldDelegateToListUsersWhenQueryBlank() {
            Page<UserAccount> page = new PageImpl<>(List.of());
            when(userAccountRepository.findAll(any(PageRequest.class))).thenReturn(page);

            CursorPage<UserResponse> result = userManagementService.searchUsers("  ", null, 10);

            verify(userAccountRepository).findAll(any(PageRequest.class));
        }

        @Test
        @DisplayName("should search by username or email when query is provided")
        void shouldSearchByUsernameOrEmail() {
            String query = "test";
            List<UserAccount> users = new ArrayList<>();
            users.add(createTestUser(UUID.randomUUID(), "testuser", "test@example.com"));

            when(userAccountRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                    eq(query), eq(query), any(Pageable.class))).thenReturn(users);

            CursorPage<UserResponse> result = userManagementService.searchUsers(query, null, 10);

            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getItems().get(0).getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("should apply cursor filtering when cursor is provided in search")
        void shouldApplyCursorFilteringInSearch() {
            String query = "user";
            UUID cursorId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            UUID afterCursorId = UUID.fromString("00000000-0000-0000-0000-000000000005");
            UUID beforeCursorId = UUID.fromString("00000000-0000-0000-0000-000000000000");

            List<UserAccount> users = new ArrayList<>();
            users.add(createTestUser(beforeCursorId, "user0", "user0@example.com"));
            users.add(createTestUser(afterCursorId, "user1", "user1@example.com"));

            when(userAccountRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                    eq(query), eq(query), any(Pageable.class))).thenReturn(users);

            CursorPage<UserResponse> result = userManagementService.searchUsers(query, cursorId.toString(), 10);

            // Only afterCursorId should be included (compareTo > 0 relative to cursorId)
            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getItems().get(0).getId()).isEqualTo(afterCursorId);
        }

        @Test
        @DisplayName("should not apply cursor filtering when cursor is empty string")
        void shouldNotApplyCursorWhenEmpty() {
            String query = "user";
            List<UserAccount> users = new ArrayList<>();
            users.add(createTestUser(UUID.randomUUID(), "user0", "user0@example.com"));

            when(userAccountRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                    eq(query), eq(query), any(Pageable.class))).thenReturn(users);

            CursorPage<UserResponse> result = userManagementService.searchUsers(query, "", 10);

            assertThat(result.getItems()).hasSize(1);
        }

        @Test
        @DisplayName("should return empty result when search has no matches")
        void shouldReturnEmptyForNoMatches() {
            String query = "nonexistent";

            when(userAccountRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                    eq(query), eq(query), any(Pageable.class))).thenReturn(List.of());

            CursorPage<UserResponse> result = userManagementService.searchUsers(query, null, 10);

            assertThat(result.getItems()).isEmpty();
            assertThat(result.isHasMore()).isFalse();
        }

        @Test
        @DisplayName("should handle hasMore correctly in search results")
        void shouldHandleHasMoreInSearch() {
            String query = "user";
            int limit = 1;

            List<UserAccount> users = new ArrayList<>();
            users.add(createTestUser(UUID.randomUUID(), "user0", "user0@example.com"));
            users.add(createTestUser(UUID.randomUUID(), "user1", "user1@example.com"));

            when(userAccountRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                    eq(query), eq(query), any(Pageable.class))).thenReturn(users);

            CursorPage<UserResponse> result = userManagementService.searchUsers(query, null, limit);

            assertThat(result.isHasMore()).isTrue();
            assertThat(result.getItems()).hasSize(limit);
        }
    }

    @Nested
    @DisplayName("updateUserRoles")
    class UpdateUserRoles {

        @Test
        @DisplayName("should update user roles successfully")
        void shouldUpdateRolesSuccessfully() {
            setupSecurityContext(ADMIN_USERNAME);
            UUID targetId = UUID.randomUUID();
            UserAccount targetUser = createTestUser(targetId, OTHER_USERNAME, "other@example.com");

            Role memberRole = new Role();
            memberRole.setId(UUID.randomUUID());
            memberRole.setName("MEMBER");

            when(userAccountRepository.findById(targetId)).thenReturn(Optional.of(targetUser));
            when(roleRepository.findByName("MEMBER")).thenReturn(Optional.of(memberRole));
            when(userAccountRepository.save(any(UserAccount.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            UserResponse response = userManagementService.updateUserRoles(targetId, Set.of("MEMBER"));

            assertThat(response.getRoles()).contains("MEMBER");
        }

        @Test
        @DisplayName("should throw BusinessException when roleNames is null")
        void shouldThrowWhenRoleNamesNull() {
            UUID targetId = UUID.randomUUID();
            UserAccount targetUser = createTestUser(targetId, OTHER_USERNAME, "other@example.com");

            when(userAccountRepository.findById(targetId)).thenReturn(Optional.of(targetUser));

            assertThatThrownBy(() -> userManagementService.updateUserRoles(targetId, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("At least one role must be specified");
        }

        @Test
        @DisplayName("should throw BusinessException when roleNames is empty")
        void shouldThrowWhenRoleNamesEmpty() {
            UUID targetId = UUID.randomUUID();
            UserAccount targetUser = createTestUser(targetId, OTHER_USERNAME, "other@example.com");

            when(userAccountRepository.findById(targetId)).thenReturn(Optional.of(targetUser));

            assertThatThrownBy(() -> userManagementService.updateUserRoles(targetId, Set.of()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("At least one role must be specified");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when role does not exist")
        void shouldThrowWhenRoleNotFound() {
            setupSecurityContext(ADMIN_USERNAME);
            UUID targetId = UUID.randomUUID();
            UserAccount targetUser = createTestUser(targetId, OTHER_USERNAME, "other@example.com");

            when(userAccountRepository.findById(targetId)).thenReturn(Optional.of(targetUser));
            when(roleRepository.findByName("NONEXISTENT")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userManagementService.updateUserRoles(targetId, Set.of("NONEXISTENT")))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Role")
                    .hasMessageContaining("NONEXISTENT");
        }

        @Test
        @DisplayName("should prevent admin from removing own ADMIN role")
        void shouldPreventSelfAdminRoleRemoval() {
            setupSecurityContext(ADMIN_USERNAME);
            UUID adminId = UUID.randomUUID();
            UserAccount adminUser = createAdminUser(adminId);

            when(userAccountRepository.findById(adminId)).thenReturn(Optional.of(adminUser));

            assertThatThrownBy(() -> userManagementService.updateUserRoles(adminId, Set.of("USER")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Cannot remove ADMIN role from your own account");
        }

        @Test
        @DisplayName("should allow admin to update own roles if ADMIN role is retained")
        void shouldAllowAdminToRetainAdminRole() {
            setupSecurityContext(ADMIN_USERNAME);
            UUID adminId = UUID.randomUUID();
            UserAccount adminUser = createAdminUser(adminId);

            Role adminRole = new Role();
            adminRole.setId(UUID.randomUUID());
            adminRole.setName("ADMIN");

            Role memberRole = new Role();
            memberRole.setId(UUID.randomUUID());
            memberRole.setName("MEMBER");

            when(userAccountRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
            when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));
            when(roleRepository.findByName("MEMBER")).thenReturn(Optional.of(memberRole));
            when(userAccountRepository.save(any(UserAccount.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            UserResponse response = userManagementService.updateUserRoles(adminId, Set.of("ADMIN", "MEMBER"));

            assertThat(response.getRoles()).contains("ADMIN", "MEMBER");
        }

        @Test
        @DisplayName("should allow removing ADMIN role from a different user")
        void shouldAllowRemovingAdminRoleFromOtherUser() {
            setupSecurityContext(ADMIN_USERNAME);
            UUID otherAdminId = UUID.randomUUID();
            UserAccount otherAdmin = createAdminUser(otherAdminId);
            otherAdmin.setUsername("otheradmin");

            Role userRole = new Role();
            userRole.setId(UUID.randomUUID());
            userRole.setName("USER");

            when(userAccountRepository.findById(otherAdminId)).thenReturn(Optional.of(otherAdmin));
            when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
            when(userAccountRepository.save(any(UserAccount.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            UserResponse response = userManagementService.updateUserRoles(otherAdminId, Set.of("USER"));

            assertThat(response.getRoles()).contains("USER");
            assertThat(response.getRoles()).doesNotContain("ADMIN");
        }

        @Test
        @DisplayName("should allow non-admin user to update roles without ADMIN restriction")
        void shouldAllowNonAdminUserRoleUpdate() {
            setupSecurityContext("regularuser");
            UUID targetId = UUID.randomUUID();
            UserAccount targetUser = createTestUser(targetId, "regularuser", "regular@example.com");

            Role userRole = new Role();
            userRole.setId(UUID.randomUUID());
            userRole.setName("USER");

            when(userAccountRepository.findById(targetId)).thenReturn(Optional.of(targetUser));
            when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
            when(userAccountRepository.save(any(UserAccount.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // The user is not currently an admin, so removing ADMIN from roleNames is fine
            UserResponse response = userManagementService.updateUserRoles(targetId, Set.of("USER"));

            assertThat(response.getRoles()).contains("USER");
        }

        @Test
        @DisplayName("should update with multiple roles")
        void shouldUpdateWithMultipleRoles() {
            setupSecurityContext(ADMIN_USERNAME);
            UUID targetId = UUID.randomUUID();
            UserAccount targetUser = createTestUser(targetId, OTHER_USERNAME, "other@example.com");

            Role adminRole = new Role();
            adminRole.setId(UUID.randomUUID());
            adminRole.setName("ADMIN");

            Role memberRole = new Role();
            memberRole.setId(UUID.randomUUID());
            memberRole.setName("MEMBER");

            when(userAccountRepository.findById(targetId)).thenReturn(Optional.of(targetUser));
            when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));
            when(roleRepository.findByName("MEMBER")).thenReturn(Optional.of(memberRole));
            when(userAccountRepository.save(any(UserAccount.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            UserResponse response = userManagementService.updateUserRoles(targetId, Set.of("ADMIN", "MEMBER"));

            assertThat(response.getRoles()).hasSize(2);
            assertThat(response.getRoles()).contains("ADMIN", "MEMBER");
        }
    }

    @Nested
    @DisplayName("deleteUser")
    class DeleteUser {

        @Test
        @DisplayName("should soft-delete user by disabling account")
        void shouldSoftDeleteUser() {
            setupSecurityContext(ADMIN_USERNAME);
            UUID adminId = UUID.randomUUID();
            UUID targetId = UUID.randomUUID();

            UserAccount adminUser = createAdminUser(adminId);
            UserAccount targetUser = createTestUser(targetId, OTHER_USERNAME, "other@example.com");

            when(userAccountRepository.findByUsername(ADMIN_USERNAME)).thenReturn(Optional.of(adminUser));
            when(userAccountRepository.findById(targetId)).thenReturn(Optional.of(targetUser));
            when(userAccountRepository.save(any(UserAccount.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            userManagementService.deleteUser(targetId);

            verify(userAccountRepository).save(targetUser);
            assertThat(targetUser.getEnabled()).isFalse();
        }

        @Test
        @DisplayName("should prevent self-deletion")
        void shouldPreventSelfDeletion() {
            setupSecurityContext(ADMIN_USERNAME);
            UUID adminId = UUID.randomUUID();
            UserAccount adminUser = createAdminUser(adminId);

            when(userAccountRepository.findByUsername(ADMIN_USERNAME)).thenReturn(Optional.of(adminUser));

            assertThatThrownBy(() -> userManagementService.deleteUser(adminId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Cannot delete your own account");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user not found")
        void shouldThrowWhenUserNotFound() {
            setupSecurityContext(ADMIN_USERNAME);
            UUID adminId = UUID.randomUUID();
            UUID targetId = UUID.randomUUID();
            UserAccount adminUser = createAdminUser(adminId);

            when(userAccountRepository.findByUsername(ADMIN_USERNAME)).thenReturn(Optional.of(adminUser));
            when(userAccountRepository.findById(targetId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userManagementService.deleteUser(targetId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should allow deletion when no authentication context")
        void shouldAllowDeletionWithNoAuth() {
            setupNullSecurityContext();
            UUID targetId = UUID.randomUUID();
            UserAccount targetUser = createTestUser(targetId, OTHER_USERNAME, "other@example.com");

            when(userAccountRepository.findById(targetId)).thenReturn(Optional.of(targetUser));
            when(userAccountRepository.save(any(UserAccount.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            userManagementService.deleteUser(targetId);

            verify(userAccountRepository).save(targetUser);
            assertThat(targetUser.getEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("preventSelfModification edge cases")
    class PreventSelfModificationEdgeCases {

        @Test
        @DisplayName("should allow action when current user is not found in repository")
        void shouldAllowWhenCurrentUserNotInRepo() {
            setupSecurityContext("unknownuser");
            UUID targetId = UUID.randomUUID();
            UserAccount targetUser = createTestUser(targetId, OTHER_USERNAME, "other@example.com");

            when(userAccountRepository.findByUsername("unknownuser")).thenReturn(Optional.empty());
            when(userAccountRepository.findById(targetId)).thenReturn(Optional.of(targetUser));
            when(userAccountRepository.save(any(UserAccount.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Should not throw even though user exists in security context but not in DB
            UserResponse response = userManagementService.deactivateUser(targetId);

            assertThat(response.getEnabled()).isFalse();
        }
    }
}
