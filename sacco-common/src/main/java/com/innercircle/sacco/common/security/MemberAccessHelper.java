package com.innercircle.sacco.common.security;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MemberAccessHelper {

    private final JdbcTemplate jdbcTemplate;

    public MemberAccessHelper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void assertAccessToMember(UUID requestedMemberId, Authentication authentication) {
        if (hasPrivilegedRole(authentication)) {
            return;
        }

        UUID currentMemberId = resolveCurrentMemberId(authentication);
        if (!requestedMemberId.equals(currentMemberId)) {
            throw new AccessDeniedException("You do not have access to this member's data");
        }
    }

    public UUID resolveCurrentUserId(Authentication authentication) {
        String principal = requirePrincipal(authentication);
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT id FROM user_accounts WHERE username = ? OR email = ?",
                    UUID.class,
                    principal,
                    principal
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new AccessDeniedException("Authenticated user account not found");
        }
    }

    public String currentActor(Authentication authentication) {
        return requirePrincipal(authentication);
    }

    private UUID resolveCurrentMemberId(Authentication authentication) {
        String principal = requirePrincipal(authentication);
        try {
            UUID memberId = jdbcTemplate.queryForObject(
                    "SELECT member_id FROM user_accounts WHERE username = ? OR email = ?",
                    UUID.class,
                    principal,
                    principal
            );
            if (memberId == null) {
                throw new AccessDeniedException("Authenticated user is not mapped to a member");
            }
            return memberId;
        } catch (EmptyResultDataAccessException ex) {
            throw new AccessDeniedException("Authenticated user account not found");
        }
    }

    private String requirePrincipal(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication is required");
        }
        String principal = authentication.getName();
        if (principal == null || principal.isBlank()) {
            throw new AccessDeniedException("Authenticated principal is missing");
        }
        return principal;
    }

    private boolean hasPrivilegedRole(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority ->
                        "ROLE_ADMIN".equals(authority)
                                || "ROLE_TREASURER".equals(authority)
                                || "ROLE_SECRETARY".equals(authority)
                                || "ROLE_CHAIRPERSON".equals(authority)
                                || "ROLE_VICE_CHAIRPERSON".equals(authority)
                                || "ROLE_VICE_TREASURER".equals(authority)
                );
    }
}
