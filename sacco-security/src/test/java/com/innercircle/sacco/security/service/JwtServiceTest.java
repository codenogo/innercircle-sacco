package com.innercircle.sacco.security.service;

import com.innercircle.sacco.security.entity.Role;
import com.innercircle.sacco.security.entity.UserAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtService")
class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // @Value fields default to null in unit tests — set to empty strings
        // so init() triggers ephemeral key generation
        ReflectionTestUtils.setField(jwtService, "rsaPublicKeyPem", "");
        ReflectionTestUtils.setField(jwtService, "rsaPrivateKeyPem", "");
        jwtService.init();
    }

    private UserAccount createUserWithRoles(String username, String... roleNames) {
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
        user.setPassword("encodedPassword");
        user.setEmail(username + "@example.com");
        user.setEnabled(true);
        user.setAccountNonLocked(true);
        user.setRoles(roles);
        return user;
    }

    @Nested
    @DisplayName("generateAccessToken")
    class GenerateAccessToken {

        @Test
        @DisplayName("should return a valid JWT with correct claims")
        void shouldReturnValidJwtWithCorrectClaims() {
            UserAccount user = createUserWithRoles("testuser", "ADMIN", "MEMBER");

            String token = jwtService.generateAccessToken(user);

            assertThat(token).isNotNull().isNotBlank();

            Jwt jwt = jwtService.decodeToken(token);
            assertThat(jwt.getSubject()).isEqualTo("testuser");
            assertThat(jwt.<String>getClaim("iss")).isEqualTo("innercircle-sacco");
            assertThat(jwt.getClaim("userId").toString()).isEqualTo(user.getId().toString());
            assertThat(jwt.getClaim("email").toString()).isEqualTo("testuser@example.com");

            List<String> roles = jwt.getClaim("roles");
            assertThat(roles).containsExactlyInAnyOrder("ADMIN", "MEMBER");

            List<String> authorities = jwt.getClaim("authorities");
            assertThat(authorities).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_MEMBER");
        }

        @Test
        @DisplayName("should set expiry to approximately 1 hour from now")
        void shouldSetExpiryToOneHour() {
            UserAccount user = createUserWithRoles("testuser", "MEMBER");
            Instant before = Instant.now().plusSeconds(3500);

            String token = jwtService.generateAccessToken(user);

            Jwt jwt = jwtService.decodeToken(token);
            Instant expiry = jwt.getExpiresAt();
            Instant after = Instant.now().plusSeconds(3700);

            assertThat(expiry).isAfter(before);
            assertThat(expiry).isBefore(after);
        }

        @Test
        @DisplayName("should set issuedAt to approximately now")
        void shouldSetIssuedAtToNow() {
            UserAccount user = createUserWithRoles("testuser", "MEMBER");
            Instant before = Instant.now().minusSeconds(5);

            String token = jwtService.generateAccessToken(user);

            Jwt jwt = jwtService.decodeToken(token);
            Instant issuedAt = jwt.getIssuedAt();
            Instant after = Instant.now().plusSeconds(5);

            assertThat(issuedAt).isAfter(before);
            assertThat(issuedAt).isBefore(after);
        }
    }

    @Nested
    @DisplayName("generateRefreshToken")
    class GenerateRefreshToken {

        @Test
        @DisplayName("should return a non-null non-empty string")
        void shouldReturnNonNullNonEmptyString() {
            String refreshToken = jwtService.generateRefreshToken();

            assertThat(refreshToken).isNotNull().isNotBlank();
        }

        @Test
        @DisplayName("should return unique tokens on successive calls")
        void shouldReturnUniqueTokens() {
            String token1 = jwtService.generateRefreshToken();
            String token2 = jwtService.generateRefreshToken();

            assertThat(token1).isNotEqualTo(token2);
        }
    }

    @Nested
    @DisplayName("getJwtDecoder")
    class GetJwtDecoder {

        @Test
        @DisplayName("should return a non-null decoder that can decode generated tokens")
        void shouldReturnWorkingDecoder() {
            UserAccount user = createUserWithRoles("testuser", "MEMBER");
            String token = jwtService.generateAccessToken(user);

            Jwt decoded = jwtService.getJwtDecoder().decode(token);

            assertThat(decoded).isNotNull();
            assertThat(decoded.getSubject()).isEqualTo("testuser");
        }
    }

    @Nested
    @DisplayName("ephemeral key fallback")
    class EphemeralKeyFallback {

        @Test
        @DisplayName("should work when no RSA config is provided")
        void shouldWorkWithoutRsaConfig() {
            // The service was initialized in @BeforeEach with no RSA config
            // Verify it can generate and decode tokens end-to-end
            UserAccount user = createUserWithRoles("ephemeral-user", "ADMIN");

            String token = jwtService.generateAccessToken(user);
            Jwt jwt = jwtService.decodeToken(token);

            assertThat(jwt.getSubject()).isEqualTo("ephemeral-user");
            assertThat(jwt.getClaim("userId").toString()).isEqualTo(user.getId().toString());
        }
    }
}
