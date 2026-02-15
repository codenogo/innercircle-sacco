package com.innercircle.sacco.reporting.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportingAuthHelperTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ReportingAuthHelper authHelper;

    // ===================== resolveCurrentMemberId tests =====================

    @Test
    void resolveCurrentMemberId_withValidEmail_shouldReturnMemberId() {
        UUID expectedId = UUID.randomUUID();
        Jwt jwt = buildJwt("user@example.com");
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jdbcTemplate.queryForObject(anyString(), eq(UUID.class), eq("user@example.com")))
                .thenReturn(expectedId);

        UUID result = authHelper.resolveCurrentMemberId(authentication);

        assertThat(result).isEqualTo(expectedId);
    }

    @Test
    void resolveCurrentMemberId_withNullEmail_shouldThrowAccessDenied() {
        Jwt jwt = buildJwt(null);
        when(authentication.getPrincipal()).thenReturn(jwt);

        assertThatThrownBy(() -> authHelper.resolveCurrentMemberId(authentication))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("No email claim in token");
    }

    @Test
    void resolveCurrentMemberId_withMemberNotFound_shouldThrowAccessDenied() {
        Jwt jwt = buildJwt("unknown@example.com");
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jdbcTemplate.queryForObject(anyString(), eq(UUID.class), eq("unknown@example.com")))
                .thenThrow(new EmptyResultDataAccessException(1));

        assertThatThrownBy(() -> authHelper.resolveCurrentMemberId(authentication))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Member not found for authenticated user");
    }

    // ===================== assertAccessToMember tests =====================

    @Test
    void assertAccessToMember_withAdminRole_shouldAllowAccess() {
        UUID requestedMemberId = UUID.randomUUID();
        when(authentication.getAuthorities()).thenReturn(
                (Collection) List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        // Should not throw
        authHelper.assertAccessToMember(requestedMemberId, authentication);

        // Should NOT call resolveCurrentMemberId (no DB query needed)
        verify(jdbcTemplate, never()).queryForObject(anyString(), eq(UUID.class), anyString());
    }

    @Test
    void assertAccessToMember_withTreasurerRole_shouldAllowAccess() {
        UUID requestedMemberId = UUID.randomUUID();
        when(authentication.getAuthorities()).thenReturn(
                (Collection) List.of(new SimpleGrantedAuthority("ROLE_TREASURER")));

        // Should not throw
        authHelper.assertAccessToMember(requestedMemberId, authentication);

        verify(jdbcTemplate, never()).queryForObject(anyString(), eq(UUID.class), anyString());
    }

    @Test
    void assertAccessToMember_withMatchingMemberId_shouldAllowAccess() {
        UUID memberId = UUID.randomUUID();
        Jwt jwt = buildJwt("member@example.com");
        when(authentication.getAuthorities()).thenReturn(
                (Collection) List.of(new SimpleGrantedAuthority("ROLE_MEMBER")));
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jdbcTemplate.queryForObject(anyString(), eq(UUID.class), eq("member@example.com")))
                .thenReturn(memberId);

        // Should not throw
        authHelper.assertAccessToMember(memberId, authentication);
    }

    @Test
    void assertAccessToMember_withDifferentMemberId_shouldThrowAccessDenied() {
        UUID requestedMemberId = UUID.randomUUID();
        UUID actualMemberId = UUID.randomUUID();
        Jwt jwt = buildJwt("other@example.com");
        when(authentication.getAuthorities()).thenReturn(
                (Collection) List.of(new SimpleGrantedAuthority("ROLE_MEMBER")));
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jdbcTemplate.queryForObject(anyString(), eq(UUID.class), eq("other@example.com")))
                .thenReturn(actualMemberId);

        assertThatThrownBy(() -> authHelper.assertAccessToMember(requestedMemberId, authentication))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You do not have access to this member's data");
    }

    @Test
    void assertAccessToMember_withNoAuthorities_shouldCheckMemberId() {
        UUID requestedMemberId = UUID.randomUUID();
        Jwt jwt = buildJwt("noauth@example.com");
        when(authentication.getAuthorities()).thenReturn(Collections.emptyList());
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jdbcTemplate.queryForObject(anyString(), eq(UUID.class), eq("noauth@example.com")))
                .thenReturn(requestedMemberId);

        // Should not throw because the member IDs match
        authHelper.assertAccessToMember(requestedMemberId, authentication);
    }

    @Test
    void assertAccessToMember_withAdminAndOtherRoles_shouldStillAllowAccess() {
        UUID requestedMemberId = UUID.randomUUID();
        when(authentication.getAuthorities()).thenReturn(
                (Collection) List.of(
                        new SimpleGrantedAuthority("ROLE_MEMBER"),
                        new SimpleGrantedAuthority("ROLE_ADMIN")));

        // Should not throw because one of the authorities is ROLE_ADMIN
        authHelper.assertAccessToMember(requestedMemberId, authentication);
    }

    @Test
    void assertAccessToMember_withNonPrivilegedRole_andMemberNotFound_shouldThrowAccessDenied() {
        UUID requestedMemberId = UUID.randomUUID();
        Jwt jwt = buildJwt("ghost@example.com");
        when(authentication.getAuthorities()).thenReturn(
                (Collection) List.of(new SimpleGrantedAuthority("ROLE_MEMBER")));
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jdbcTemplate.queryForObject(anyString(), eq(UUID.class), eq("ghost@example.com")))
                .thenThrow(new EmptyResultDataAccessException(1));

        assertThatThrownBy(() -> authHelper.assertAccessToMember(requestedMemberId, authentication))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Member not found for authenticated user");
    }

    // ===================== helper methods =====================

    private Jwt buildJwt(String email) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", "user-123")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600));
        if (email != null) {
            builder.claim("email", email);
        }
        return builder.build();
    }
}
