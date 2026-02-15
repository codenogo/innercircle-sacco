package com.innercircle.sacco.security.service;

import com.innercircle.sacco.security.entity.Role;
import com.innercircle.sacco.security.entity.UserAccount;
import com.innercircle.sacco.security.repository.UserAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SaccoUserDetailsService")
class SaccoUserDetailsServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private SaccoUserDetailsService saccoUserDetailsService;

    private UserAccount createUserWithRoles(String username, boolean enabled, boolean accountNonLocked,
                                             String... roleNames) {
        Set<Role> roles = new HashSet<>();
        for (String roleName : roleNames) {
            Role role = new Role();
            role.setId(UUID.randomUUID());
            role.setName(roleName);
            roles.add(role);
        }

        UserAccount user = new UserAccount();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setPassword("encodedPassword123");
        user.setEmail(username + "@example.com");
        user.setEnabled(enabled);
        user.setAccountNonLocked(accountNonLocked);
        user.setRoles(roles);
        return user;
    }

    @Nested
    @DisplayName("loadUserByUsername")
    class LoadUserByUsername {

        @Test
        @DisplayName("should load user details successfully for existing user")
        void shouldLoadUserDetailsSuccessfully() {
            UserAccount user = createUserWithRoles("testuser", true, true, "USER");
            when(userAccountRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

            UserDetails userDetails = saccoUserDetailsService.loadUserByUsername("testuser");

            assertThat(userDetails.getUsername()).isEqualTo("testuser");
            assertThat(userDetails.getPassword()).isEqualTo("encodedPassword123");
            assertThat(userDetails.isEnabled()).isTrue();
            assertThat(userDetails.isAccountNonLocked()).isTrue();
            verify(userAccountRepository).findByUsername("testuser");
        }

        @Test
        @DisplayName("should throw UsernameNotFoundException when user does not exist")
        void shouldThrowWhenUserNotFound() {
            when(userAccountRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> saccoUserDetailsService.loadUserByUsername("nonexistent"))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("User not found: nonexistent");
        }

        @Test
        @DisplayName("should map roles to ROLE_ prefixed authorities")
        void shouldMapRolesToAuthorities() {
            UserAccount user = createUserWithRoles("testuser", true, true, "ADMIN", "USER");
            when(userAccountRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

            UserDetails userDetails = saccoUserDetailsService.loadUserByUsername("testuser");

            Set<String> authorities = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());

            assertThat(authorities).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
        }

        @Test
        @DisplayName("should set disabled flag when user is not enabled")
        void shouldSetDisabledWhenNotEnabled() {
            UserAccount user = createUserWithRoles("disableduser", false, true, "USER");
            when(userAccountRepository.findByUsername("disableduser")).thenReturn(Optional.of(user));

            UserDetails userDetails = saccoUserDetailsService.loadUserByUsername("disableduser");

            assertThat(userDetails.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("should set account locked flag when account is locked")
        void shouldSetLockedWhenAccountLocked() {
            UserAccount user = createUserWithRoles("lockeduser", true, false, "USER");
            when(userAccountRepository.findByUsername("lockeduser")).thenReturn(Optional.of(user));

            UserDetails userDetails = saccoUserDetailsService.loadUserByUsername("lockeduser");

            assertThat(userDetails.isAccountNonLocked()).isFalse();
        }

        @Test
        @DisplayName("should handle user with no roles")
        void shouldHandleUserWithNoRoles() {
            UserAccount user = createUserWithRoles("noroleuser", true, true);
            when(userAccountRepository.findByUsername("noroleuser")).thenReturn(Optional.of(user));

            UserDetails userDetails = saccoUserDetailsService.loadUserByUsername("noroleuser");

            assertThat(userDetails.getAuthorities()).isEmpty();
        }

        @Test
        @DisplayName("should handle user with single role")
        void shouldHandleUserWithSingleRole() {
            UserAccount user = createUserWithRoles("singlerole", true, true, "MEMBER");
            when(userAccountRepository.findByUsername("singlerole")).thenReturn(Optional.of(user));

            UserDetails userDetails = saccoUserDetailsService.loadUserByUsername("singlerole");

            assertThat(userDetails.getAuthorities()).hasSize(1);
            assertThat(userDetails.getAuthorities().iterator().next().getAuthority())
                    .isEqualTo("ROLE_MEMBER");
        }

        @Test
        @DisplayName("should handle user with multiple roles")
        void shouldHandleUserWithMultipleRoles() {
            UserAccount user = createUserWithRoles("multirole", true, true, "ADMIN", "USER", "MEMBER");
            when(userAccountRepository.findByUsername("multirole")).thenReturn(Optional.of(user));

            UserDetails userDetails = saccoUserDetailsService.loadUserByUsername("multirole");

            assertThat(userDetails.getAuthorities()).hasSize(3);
            Set<String> authorities = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());
            assertThat(authorities).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER", "ROLE_MEMBER");
        }

        @Test
        @DisplayName("should handle disabled and locked user simultaneously")
        void shouldHandleDisabledAndLockedUser() {
            UserAccount user = createUserWithRoles("bothflags", false, false, "USER");
            when(userAccountRepository.findByUsername("bothflags")).thenReturn(Optional.of(user));

            UserDetails userDetails = saccoUserDetailsService.loadUserByUsername("bothflags");

            assertThat(userDetails.isEnabled()).isFalse();
            assertThat(userDetails.isAccountNonLocked()).isFalse();
        }

        @Test
        @DisplayName("should pass correct username to repository")
        void shouldPassCorrectUsernameToRepository() {
            String username = "specific.user@name";
            when(userAccountRepository.findByUsername(username)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> saccoUserDetailsService.loadUserByUsername(username))
                    .isInstanceOf(UsernameNotFoundException.class);

            verify(userAccountRepository).findByUsername(username);
        }
    }
}
