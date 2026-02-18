package com.innercircle.sacco.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberAccessHelperTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private Authentication authentication;

    private MemberAccessHelper helper;

    @BeforeEach
    void setUp() {
        helper = new MemberAccessHelper(jdbcTemplate);
        lenient().when(authentication.isAuthenticated()).thenReturn(true);
        lenient().when(authentication.getName()).thenReturn("member1");
    }

    @Test
    void assertAccessToMember_allowsPrivilegedRoles() {
        UUID requestedMemberId = UUID.randomUUID();
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        doReturn(authorities).when(authentication).getAuthorities();

        helper.assertAccessToMember(requestedMemberId, authentication);

        verify(jdbcTemplate, never()).queryForObject(anyString(), eq(UUID.class), anyString(), anyString());
    }

    @Test
    void assertAccessToMember_allowsCurrentMember() {
        UUID requestedMemberId = UUID.randomUUID();
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_MEMBER"));
        doReturn(authorities).when(authentication).getAuthorities();
        when(jdbcTemplate.queryForObject(
                "SELECT member_id FROM user_accounts WHERE username = ? OR email = ?",
                UUID.class,
                "member1",
                "member1"
        )).thenReturn(requestedMemberId);

        helper.assertAccessToMember(requestedMemberId, authentication);
    }

    @Test
    void assertAccessToMember_rejectsDifferentMember() {
        UUID requestedMemberId = UUID.randomUUID();
        UUID actualMemberId = UUID.randomUUID();
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_MEMBER"));
        doReturn(authorities).when(authentication).getAuthorities();
        when(jdbcTemplate.queryForObject(
                "SELECT member_id FROM user_accounts WHERE username = ? OR email = ?",
                UUID.class,
                "member1",
                "member1"
        )).thenReturn(actualMemberId);

        assertThatThrownBy(() -> helper.assertAccessToMember(requestedMemberId, authentication))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("do not have access");
    }

    @Test
    void resolveCurrentUserId_returnsMappedUser() {
        UUID userId = UUID.randomUUID();
        when(jdbcTemplate.queryForObject(
                "SELECT id FROM user_accounts WHERE username = ? OR email = ?",
                UUID.class,
                "member1",
                "member1"
        )).thenReturn(userId);

        UUID result = helper.resolveCurrentUserId(authentication);

        assertThat(result).isEqualTo(userId);
    }
}
