package com.innercircle.sacco.security.service;

import com.innercircle.sacco.security.dto.LoginRequest;
import com.innercircle.sacco.security.dto.LoginResponse;
import com.innercircle.sacco.security.dto.RefreshTokenRequest;
import com.innercircle.sacco.security.entity.RefreshToken;
import com.innercircle.sacco.security.entity.Role;
import com.innercircle.sacco.security.entity.UserAccount;
import com.innercircle.sacco.security.repository.RefreshTokenRepository;
import com.innercircle.sacco.security.repository.UserAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock
    private SaccoUserDetailsService userDetailsService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private AuthService authService;

    private UserAccount createUserAccount(UUID id, String username, boolean enabled, boolean accountNonLocked,
                                           String... roleNames) {
        Set<Role> roles = new HashSet<>();
        for (String roleName : roleNames) {
            Role role = new Role();
            role.setId(UUID.randomUUID());
            role.setName(roleName);
            roles.add(role);
        }

        UserAccount user = new UserAccount();
        user.setId(id);
        user.setUsername(username);
        user.setPassword("encodedPassword");
        user.setEmail(username + "@example.com");
        user.setEnabled(enabled);
        user.setAccountNonLocked(accountNonLocked);
        user.setRoles(roles);
        return user;
    }

    private UserDetails createUserDetails(String username, String password, boolean enabled, boolean accountNonLocked,
                                           String... roles) {
        var authorities = new HashSet<SimpleGrantedAuthority>();
        for (String role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        return User.builder()
                .username(username)
                .password(password)
                .disabled(!enabled)
                .accountLocked(!accountNonLocked)
                .authorities(authorities)
                .build();
    }

    @Nested
    @DisplayName("authenticate")
    class Authenticate {

        @Test
        @DisplayName("should return LoginResponse with tokens on successful authentication")
        void shouldReturnTokensOnSuccess() {
            UUID userId = UUID.randomUUID();
            LoginRequest request = new LoginRequest("testuser", "password123");
            UserDetails userDetails = createUserDetails("testuser", "encodedPassword", true, true, "MEMBER");
            UserAccount userAccount = createUserAccount(userId, "testuser", true, true, "MEMBER");

            when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
            when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
            when(userAccountRepository.findByUsername("testuser")).thenReturn(Optional.of(userAccount));
            when(jwtService.generateAccessToken(userAccount)).thenReturn("access-token-value");
            when(jwtService.generateRefreshToken()).thenReturn("refresh-token-value");
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

            LoginResponse response = authService.authenticate(request);

            assertThat(response.accessToken()).isEqualTo("access-token-value");
            assertThat(response.refreshToken()).isEqualTo("refresh-token-value");
            assertThat(response.tokenType()).isEqualTo("Bearer");
            assertThat(response.expiresIn()).isEqualTo(3600L);
            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("should throw BadCredentialsException when password is wrong")
        void shouldThrowWhenPasswordIsWrong() {
            LoginRequest request = new LoginRequest("testuser", "wrongpassword");
            UserDetails userDetails = createUserDetails("testuser", "encodedPassword", true, true, "MEMBER");

            when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
            when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

            assertThatThrownBy(() -> authService.authenticate(request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Invalid username or password");
        }

        @Test
        @DisplayName("should throw DisabledException when account is disabled")
        void shouldThrowWhenAccountDisabled() {
            LoginRequest request = new LoginRequest("disableduser", "password123");
            UserDetails userDetails = createUserDetails("disableduser", "encodedPassword", false, true, "MEMBER");

            when(userDetailsService.loadUserByUsername("disableduser")).thenReturn(userDetails);

            assertThatThrownBy(() -> authService.authenticate(request))
                    .isInstanceOf(DisabledException.class)
                    .hasMessage("Account is disabled");
        }

        @Test
        @DisplayName("should throw LockedException when account is locked")
        void shouldThrowWhenAccountLocked() {
            LoginRequest request = new LoginRequest("lockeduser", "password123");
            UserDetails userDetails = createUserDetails("lockeduser", "encodedPassword", true, false, "MEMBER");

            when(userDetailsService.loadUserByUsername("lockeduser")).thenReturn(userDetails);

            assertThatThrownBy(() -> authService.authenticate(request))
                    .isInstanceOf(LockedException.class)
                    .hasMessage("Account is locked");
        }
    }

    @Nested
    @DisplayName("refreshAccessToken")
    class RefreshAccessToken {

        @Test
        @DisplayName("should return new tokens on valid refresh token")
        void shouldReturnNewTokensOnValidRefreshToken() {
            UUID userId = UUID.randomUUID();
            RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");

            RefreshToken existingToken = RefreshToken.builder()
                    .userId(userId)
                    .token("valid-refresh-token")
                    .expiresAt(Instant.now().plusSeconds(86400))
                    .revoked(false)
                    .build();
            existingToken.setId(UUID.randomUUID());

            UserAccount userAccount = createUserAccount(userId, "testuser", true, true, "MEMBER");

            when(refreshTokenRepository.findByToken("valid-refresh-token")).thenReturn(Optional.of(existingToken));
            when(refreshTokenRepository.rotateIfActive(any(UUID.class), any(UUID.class), any(Instant.class), any(Instant.class)))
                    .thenReturn(1);
            when(userAccountRepository.findById(userId)).thenReturn(Optional.of(userAccount));
            when(jwtService.generateAccessToken(userAccount)).thenReturn("new-access-token");
            when(jwtService.generateRefreshToken()).thenReturn("new-refresh-token");
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> {
                RefreshToken token = inv.getArgument(0);
                if (token.getId() == null) {
                    token.setId(UUID.randomUUID());
                }
                return token;
            });

            LoginResponse response = authService.refreshAccessToken(request);

            assertThat(response.accessToken()).isEqualTo("new-access-token");
            assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
            assertThat(response.tokenType()).isEqualTo("Bearer");
            assertThat(response.expiresIn()).isEqualTo(3600L);
            verify(refreshTokenRepository).rotateIfActive(
                    eq(existingToken.getId()),
                    any(UUID.class),
                    any(Instant.class),
                    any(Instant.class)
            );
        }

        @Test
        @DisplayName("should throw BadCredentialsException when refresh token is expired")
        void shouldThrowWhenRefreshTokenExpired() {
            RefreshTokenRequest request = new RefreshTokenRequest("expired-token");

            RefreshToken expiredToken = RefreshToken.builder()
                    .userId(UUID.randomUUID())
                    .token("expired-token")
                    .expiresAt(Instant.now().minusSeconds(3600))
                    .revoked(false)
                    .build();

            when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken));

            assertThatThrownBy(() -> authService.refreshAccessToken(request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Refresh token expired");
        }

        @Test
        @DisplayName("should throw BadCredentialsException when refresh token is revoked")
        void shouldThrowWhenRefreshTokenRevoked() {
            RefreshTokenRequest request = new RefreshTokenRequest("revoked-token");

            RefreshToken revokedToken = RefreshToken.builder()
                    .userId(UUID.randomUUID())
                    .token("revoked-token")
                    .expiresAt(Instant.now().plusSeconds(86400))
                    .revoked(true)
                    .build();

            when(refreshTokenRepository.findByToken("revoked-token")).thenReturn(Optional.of(revokedToken));

            assertThatThrownBy(() -> authService.refreshAccessToken(request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Invalid refresh token");
        }

        @Test
        @DisplayName("should return latest token when already-revoked token is retried within grace period")
        void shouldReturnLatestTokenWhenAlreadyRevokedTokenRetriedWithinGracePeriod() {
            RefreshTokenRequest request = new RefreshTokenRequest("revoked-token");
            UUID userId = UUID.randomUUID();
            UUID replacementTokenId = UUID.randomUUID();

            RefreshToken revokedToken = RefreshToken.builder()
                    .userId(userId)
                    .token("revoked-token")
                    .expiresAt(Instant.now().plusSeconds(86400))
                    .revoked(true)
                    .replacedByTokenId(replacementTokenId)
                    .revokedAt(Instant.now())
                    .build();

            RefreshToken replacementToken = RefreshToken.builder()
                    .userId(userId)
                    .token("replacement-refresh-token")
                    .expiresAt(Instant.now().plusSeconds(86400))
                    .revoked(false)
                    .build();
            replacementToken.setId(replacementTokenId);

            UserAccount userAccount = createUserAccount(userId, "testuser", true, true, "MEMBER");

            when(refreshTokenRepository.findByToken("revoked-token")).thenReturn(Optional.of(revokedToken));
            when(refreshTokenRepository.findById(replacementTokenId)).thenReturn(Optional.of(replacementToken));
            when(userAccountRepository.findById(userId)).thenReturn(Optional.of(userAccount));
            when(jwtService.generateAccessToken(userAccount)).thenReturn("new-access-token");

            LoginResponse response = authService.refreshAccessToken(request);

            assertThat(response.accessToken()).isEqualTo("new-access-token");
            assertThat(response.refreshToken()).isEqualTo("replacement-refresh-token");
            verify(refreshTokenRepository, never()).rotateIfActive(any(UUID.class), any(UUID.class), any(Instant.class), any(Instant.class));
        }

        @Test
        @DisplayName("should throw BadCredentialsException when refresh token is unknown")
        void shouldThrowWhenRefreshTokenUnknown() {
            RefreshTokenRequest request = new RefreshTokenRequest("unknown-token");

            when(refreshTokenRepository.findByToken("unknown-token")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refreshAccessToken(request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Invalid refresh token");
        }

        @Test
        @DisplayName("should return latest token when refresh token is consumed concurrently")
        void shouldReturnLatestTokenWhenRefreshTokenConsumedConcurrently() {
            RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");
            UUID userId = UUID.randomUUID();
            UUID originalTokenId = UUID.randomUUID();
            UUID replacementTokenId = UUID.randomUUID();

            RefreshToken existingToken = RefreshToken.builder()
                    .userId(userId)
                    .token("valid-refresh-token")
                    .expiresAt(Instant.now().plusSeconds(86400))
                    .revoked(false)
                    .build();
            existingToken.setId(originalTokenId);

            RefreshToken rotatedTokenState = RefreshToken.builder()
                    .userId(userId)
                    .token("valid-refresh-token")
                    .expiresAt(Instant.now().plusSeconds(86400))
                    .revoked(true)
                    .replacedByTokenId(replacementTokenId)
                    .revokedAt(Instant.now())
                    .build();
            rotatedTokenState.setId(originalTokenId);

            RefreshToken replacementToken = RefreshToken.builder()
                    .userId(userId)
                    .token("replacement-refresh-token")
                    .expiresAt(Instant.now().plusSeconds(86400))
                    .revoked(false)
                    .build();
            replacementToken.setId(replacementTokenId);

            UserAccount userAccount = createUserAccount(userId, "testuser", true, true, "MEMBER");

            when(refreshTokenRepository.findByToken("valid-refresh-token")).thenReturn(Optional.of(existingToken));
            when(refreshTokenRepository.rotateIfActive(any(UUID.class), any(UUID.class), any(Instant.class), any(Instant.class)))
                    .thenReturn(0);
            when(refreshTokenRepository.findById(originalTokenId)).thenReturn(Optional.of(rotatedTokenState));
            when(refreshTokenRepository.findById(replacementTokenId)).thenReturn(Optional.of(replacementToken));
            when(userAccountRepository.findById(userId)).thenReturn(Optional.of(userAccount));
            when(jwtService.generateAccessToken(userAccount)).thenReturn("new-access-token");
            when(jwtService.generateRefreshToken()).thenReturn("unused-refresh-token");
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> {
                RefreshToken token = inv.getArgument(0);
                if (token.getId() == null) {
                    token.setId(UUID.randomUUID());
                }
                return token;
            });

            LoginResponse response = authService.refreshAccessToken(request);

            assertThat(response.accessToken()).isEqualTo("new-access-token");
            assertThat(response.refreshToken()).isEqualTo("replacement-refresh-token");
        }

        @Test
        @DisplayName("should throw BadCredentialsException when consumed token is retried after grace period")
        void shouldThrowWhenConsumedTokenRetriedAfterGracePeriod() {
            RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");
            UUID originalTokenId = UUID.randomUUID();
            UUID replacementTokenId = UUID.randomUUID();

            RefreshToken existingToken = RefreshToken.builder()
                    .userId(UUID.randomUUID())
                    .token("valid-refresh-token")
                    .expiresAt(Instant.now().plusSeconds(86400))
                    .revoked(false)
                    .build();
            existingToken.setId(originalTokenId);

            RefreshToken rotatedTokenState = RefreshToken.builder()
                    .userId(existingToken.getUserId())
                    .token("valid-refresh-token")
                    .expiresAt(Instant.now().plusSeconds(86400))
                    .revoked(true)
                    .replacedByTokenId(replacementTokenId)
                    .revokedAt(Instant.now().minusSeconds(60))
                    .build();
            rotatedTokenState.setId(originalTokenId);

            when(refreshTokenRepository.findByToken("valid-refresh-token")).thenReturn(Optional.of(existingToken));
            when(refreshTokenRepository.rotateIfActive(any(UUID.class), any(UUID.class), any(Instant.class), any(Instant.class)))
                    .thenReturn(0);
            when(refreshTokenRepository.findById(originalTokenId)).thenReturn(Optional.of(rotatedTokenState));
            when(jwtService.generateRefreshToken()).thenReturn("unused-refresh-token");
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> {
                RefreshToken token = inv.getArgument(0);
                if (token.getId() == null) {
                    token.setId(UUID.randomUUID());
                }
                return token;
            });

            assertThatThrownBy(() -> authService.refreshAccessToken(request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Invalid refresh token");
        }
    }
}
